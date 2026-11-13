// Gera o modelo PROVISÓRIO do Fox (app/src/main/assets/car/fox.glb) até chegar o modelo comprado.
// Maquete low-poly com as medidas do Fox G2 (3,823 × 1,640 × 1,544 m, entre-eixos 2,465 m) e as peças
// móveis separadas com os nomes do contrato (docs/car3d/contrato-do-modelo.md): o app anima pelos nomes.
//
// Uso: node tools/car3d/make-placeholder-fox.mjs [saída.glb]
// Eixos glTF: +Y para cima, frente do carro em +Z, lado esquerdo (motorista) em +X. Origem no chão, no
// meio do carro. Unidades em metros.

import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const out = process.argv[2] ?? resolve(here, "../../app/src/main/assets/car/fox.glb");

// ---------- materiais ----------
const MATERIALS = [
  { name: "paint", baseColorFactor: [0.63, 0.65, 0.68, 1], metallicFactor: 0.7, roughnessFactor: 0.3 },
  { name: "glass", baseColorFactor: [0.05, 0.06, 0.07, 1], metallicFactor: 0.0, roughnessFactor: 0.1 },
  { name: "trim", baseColorFactor: [0.04, 0.04, 0.045, 1], metallicFactor: 0.0, roughnessFactor: 0.6 },
  { name: "tire", baseColorFactor: [0.06, 0.06, 0.06, 1], metallicFactor: 0.0, roughnessFactor: 0.9 },
  { name: "rim", baseColorFactor: [0.72, 0.74, 0.76, 1], metallicFactor: 0.9, roughnessFactor: 0.3 },
  { name: "lamp_head", baseColorFactor: [0.75, 0.78, 0.82, 1], metallicFactor: 0.0, roughnessFactor: 0.2 },
  { name: "lamp_tail", baseColorFactor: [0.45, 0.04, 0.05, 1], metallicFactor: 0.0, roughnessFactor: 0.2 },
  { name: "lamp_turn", baseColorFactor: [0.55, 0.33, 0.05, 1], metallicFactor: 0.0, roughnessFactor: 0.2 },
  { name: "plate", baseColorFactor: [0.92, 0.92, 0.92, 1], metallicFactor: 0.0, roughnessFactor: 0.6 },
];
const M = Object.fromEntries(MATERIALS.map((m, i) => [m.name, i]));

// ---------- malha com normais por face ----------
class Mesh {
  constructor() { this.prims = new Map(); }
  prim(mat) {
    if (!this.prims.has(mat)) this.prims.set(mat, { pos: [], nrm: [], idx: [] });
    return this.prims.get(mat);
  }
  /** Triângulo com normal da face; [hint] = direção "para fora" (acerta a ordem dos vértices). */
  /** [exact] = usa [hint] como normal (lateral levemente curva sem vincos de triangulação). */
  tri(mat, a, b, c, hint, exact = false) {
    let n = cross(sub(b, a), sub(c, a));
    if (len(n) < 1e-12) return;
    if (hint && dot(n, hint) < 0) { [b, c] = [c, b]; n = scale(n, -1); }
    n = exact && hint ? norm(hint) : norm(n);
    const p = this.prim(mat);
    const base = p.pos.length / 3;
    for (const v of [a, b, c]) { p.pos.push(...v); p.nrm.push(...n); }
    p.idx.push(base, base + 1, base + 2);
  }
  quad(mat, a, b, c, d, hint) { this.tri(mat, a, b, c, hint); this.tri(mat, a, c, d, hint); }
  box(mat, [cx, cy, cz], [sx, sy, sz]) {
    const x0 = cx - sx / 2, x1 = cx + sx / 2, y0 = cy - sy / 2, y1 = cy + sy / 2, z0 = cz - sz / 2, z1 = cz + sz / 2;
    this.quad(mat, [x1, y0, z0], [x1, y1, z0], [x1, y1, z1], [x1, y0, z1], [1, 0, 0]);
    this.quad(mat, [x0, y0, z0], [x0, y1, z0], [x0, y1, z1], [x0, y0, z1], [-1, 0, 0]);
    this.quad(mat, [x0, y1, z0], [x1, y1, z0], [x1, y1, z1], [x0, y1, z1], [0, 1, 0]);
    this.quad(mat, [x0, y0, z0], [x1, y0, z0], [x1, y0, z1], [x0, y0, z1], [0, -1, 0]);
    this.quad(mat, [x0, y0, z1], [x1, y0, z1], [x1, y1, z1], [x0, y1, z1], [0, 0, 1]);
    this.quad(mat, [x0, y0, z0], [x1, y0, z0], [x1, y1, z0], [x0, y1, z0], [0, 0, -1]);
  }
  /**
   * Chapa: polígono 2D (u,v) levado ao 3D por [map](u, v, o), com o = 0 (face de dentro) e o = 1 (face de
   * fora). [outward](u,v) = direção de fora (para acertar a ordem dos vértices).
   */
  slab(mat, poly, map, outward, exactCaps = false) {
    const ring = ccw(poly);
    for (const [i, j, k] of earclip(ring)) {
      const [a, b, c] = [ring[i], ring[j], ring[k]];
      this.tri(mat, map(...a, 1), map(...b, 1), map(...c, 1), outward(...a), exactCaps);
      this.tri(mat, map(...a, 0), map(...b, 0), map(...c, 0), scale(outward(...a), -1), exactCaps);
    }
    for (let i = 0; i < ring.length; i++) {
      const a = ring[i], b = ring[(i + 1) % ring.length];
      // Normal da borda no plano 2D (polígono anti-horário: a de fora fica à direita).
      const e = [b[0] - a[0], b[1] - a[1]];
      const n2 = [e[1], -e[0]];
      const pa0 = map(...a, 0), pa1 = map(...a, 1), pb0 = map(...b, 0), pb1 = map(...b, 1);
      const mid = scale(add(add(pa0, pb1), add(pa1, pb0)), 0.25);
      const out2 = sub(map(a[0] + n2[0] * 0.01, a[1] + n2[1] * 0.01, 0.5), map(...a, 0.5));
      this.quad(mat, pa0, pb0, pb1, pa1, len(out2) > 0 ? out2 : mid);
    }
  }
  /** Cilindro deitado no eixo X (pneu), centrado na origem. */
  cylinderX(mat, r, w, seg, x0 = -w / 2) {
    const x1 = x0 + w;
    for (let i = 0; i < seg; i++) {
      const a0 = (i / seg) * 2 * Math.PI, a1 = ((i + 1) / seg) * 2 * Math.PI;
      const p = (x, a) => [x, r * Math.sin(a), r * Math.cos(a)];
      const am = (a0 + a1) / 2;
      this.quad(mat, p(x0, a0), p(x1, a0), p(x1, a1), p(x0, a1), [0, Math.sin(am), Math.cos(am)]);
      this.tri(mat, [x1, 0, 0], p(x1, a0), p(x1, a1), [1, 0, 0]);
      this.tri(mat, [x0, 0, 0], p(x0, a0), p(x0, a1), [-1, 0, 0]);
    }
  }
  /** Copia a malha deslocada por -[origin] (vértices relativos ao pivô do nó). */
  relativeTo(origin) {
    const m = new Mesh();
    for (const [mat, p] of this.prims) {
      const q = m.prim(mat);
      for (let i = 0; i < p.pos.length; i += 3) q.pos.push(p.pos[i] - origin[0], p.pos[i + 1] - origin[1], p.pos[i + 2] - origin[2]);
      q.nrm.push(...p.nrm);
      q.idx.push(...p.idx);
    }
    return m;
  }
  /** Espelha no X (lado esquerdo → direito). */
  mirrorX() {
    const m = new Mesh();
    for (const [mat, p] of this.prims) {
      const q = m.prim(mat);
      for (let i = 0; i < p.pos.length; i += 3) {
        q.pos.push(-p.pos[i], p.pos[i + 1], p.pos[i + 2]);
        q.nrm.push(-p.nrm[i], p.nrm[i + 1], p.nrm[i + 2]);
      }
      for (let i = 0; i < p.idx.length; i += 3) q.idx.push(p.idx[i], p.idx[i + 2], p.idx[i + 1]);
    }
    return m;
  }
}

// ---------- geometria 2D ----------
const add = (a, b) => a.map((v, i) => v + b[i]);
const sub = (a, b) => a.map((v, i) => v - b[i]);
const scale = (a, s) => a.map((v) => v * s);
const dot = (a, b) => a.reduce((s, v, i) => s + v * b[i], 0);
const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
const len = (a) => Math.sqrt(dot(a, a));
const norm = (a) => scale(a, 1 / len(a));
const area2 = (p) => p.reduce((s, a, i) => { const b = p[(i + 1) % p.length]; return s + a[0] * b[1] - b[0] * a[1]; }, 0);
const ccw = (p) => (area2(p) < 0 ? [...p].reverse() : p);

function chaikin(p, iters) {
  for (let k = 0; k < iters; k++) {
    const q = [];
    for (let i = 0; i < p.length; i++) {
      const a = p[i], b = p[(i + 1) % p.length];
      q.push([0.75 * a[0] + 0.25 * b[0], 0.75 * a[1] + 0.25 * b[1]], [0.25 * a[0] + 0.75 * b[0], 0.25 * a[1] + 0.75 * b[1]]);
    }
    p = q;
  }
  return p;
}

/** Triangulação por "orelhas" de um polígono simples anti-horário; devolve trios de índices. */
function earclip(p) {
  const idx = p.map((_, i) => i);
  const tris = [];
  const cr = (a, b, c) => (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
  const inside = (q, a, b, c) => cr(a, b, q) >= 0 && cr(b, c, q) >= 0 && cr(c, a, q) >= 0;
  let guard = 0;
  while (idx.length > 3 && guard++ < 100000) {
    let clipped = false;
    for (let i = 0; i < idx.length; i++) {
      const i0 = idx[(i + idx.length - 1) % idx.length], i1 = idx[i], i2 = idx[(i + 1) % idx.length];
      const a = p[i0], b = p[i1], c = p[i2];
      if (cr(a, b, c) <= 1e-12) continue;
      let ok = true;
      for (const j of idx) {
        if (j === i0 || j === i1 || j === i2) continue;
        if (inside(p[j], a, b, c)) { ok = false; break; }
      }
      if (!ok) continue;
      tris.push([i0, i1, i2]);
      idx.splice(i, 1);
      clipped = true;
      break;
    }
    if (!clipped) { // polígono degenerado: corta mesmo assim pra não travar
      tris.push([idx[0], idx[1], idx[2]]);
      idx.splice(1, 1);
    }
  }
  if (idx.length === 3) tris.push([idx[0], idx[1], idx[2]]);
  return tris;
}

const lerp = (a, b, t) => a + (b - a) * t;
const smooth = (e0, e1, x) => { const t = Math.min(1, Math.max(0, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); };
/** Interpolação linear numa tabela [[x, y], ...] (x crescente). */
function table(t, x) {
  if (x <= t[0][0]) return t[0][1];
  for (let i = 1; i < t.length; i++) if (x <= t[i][0]) return lerp(t[i - 1][1], t[i][1], (x - t[i - 1][0]) / (t[i][0] - t[i - 1][0]));
  return t[t.length - 1][1];
}

// ---------- medidas ----------
const FRONT_AXLE = 1.165, REAR_AXLE = -1.3; // entre-eixos 2,465
const TRACK_HALF = 0.70, WHEEL_R = 0.30, WHEEL_W = 0.19, ARCH_R = 0.36;
/** Cantos arredondados vistos de cima: estreita perto da frente e da traseira. */
const taper = (z) => 1 - 0.09 * smooth(1.5, 1.95, Math.abs(z));
const bodyHalf = (y, z) => (y < 0.85 ? 0.8 : 0.8 - (y - 0.85) * 0.17) * taper(z);
const glassHalf = (y, z) => (0.77 - (y - 0.98) * 0.236) * taper(z);

function arch(zc, from, to, steps = 10) {
  const pts = [];
  for (let i = 0; i <= steps; i++) {
    const a = ((from + (to - from) * (i / steps)) * Math.PI) / 180;
    pts.push([zc + ARCH_R * Math.cos(a), 0.3 + ARCH_R * Math.sin(a)]);
  }
  return pts;
}

// Perfil lateral (z, y) da carroceria abaixo da linha de cintura, com as caixas de roda.
const bodyProfile = chaikin([
  [1.8, 0.22], [1.9, 0.32], [1.93, 0.55], [1.88, 0.68], [1.78, 0.8], [1.3, 0.9], [0.95, 0.98],
  [-1.6, 1.01], [-1.88, 1.03], [-1.94, 0.75], [-1.93, 0.45], [-1.88, 0.26], [-1.75, 0.22],
  ...arch(REAR_AXLE, 190, -10), ...arch(FRONT_AXLE, 190, -10),
], 2);
const glassProfile = chaikin([
  [0.95, 0.97], [0.25, 1.48], [0.05, 1.53], [-1.5, 1.54], [-1.8, 1.4], [-1.9, 1.04], [-1.6, 0.97],
], 1);

/** Extrusão do perfil (z, y) na largura, meia-largura [half](y, z). */
function extrude(mesh, mat, profile, half) {
  const ring = ccw(profile);
  for (const s of [1, -1]) {
    for (const [i, j, k] of earclip(ring)) {
      const v = (q) => [s * half(q[1], q[0]), q[1], q[0]];
      mesh.tri(mat, v(ring[i]), v(ring[j]), v(ring[k]), [s, 0, 0], true);
    }
  }
  for (let i = 0; i < ring.length; i++) {
    const a = ring[i], b = ring[(i + 1) % ring.length];
    const pa = (s) => [s * half(a[1], a[0]), a[1], a[0]];
    const pb = (s) => [s * half(b[1], b[0]), b[1], b[0]];
    // Anti-horário em (z, y): a normal de fora da borda a→b é (dy, -dz) em (z, y).
    const n = [0, -(b[0] - a[0]), b[1] - a[1]];
    mesh.quad(mat, pa(1), pb(1), pb(-1), pa(-1), n);
  }
}

// ---------- peças ----------
const body = new Mesh();
extrude(body, M.paint, bodyProfile, bodyHalf);
extrude(body, M.glass, glassProfile, glassHalf);
// Teto pintado por cima do vidro.
extrude(body, M.paint, [[0.22, 1.49], [0.1, 1.548], [-1.5, 1.558], [-1.63, 1.5]], () => 0.648);
// Coluna C pintada (cada lado), sobre o vidro.
for (const s of [1, -1]) {
  body.slab(M.paint, chaikin([[-1.08, 0.98], [-1.02, 1.5], [-1.55, 1.53], [-1.83, 1.37], [-1.91, 1.02]], 1),
    (z, y, o) => [s * (glassHalf(y, z) + 0.004 + o * 0.012), y, z], () => [s, 0, 0], true);
}
// Grades, placas em branco e o engate (o Fox do dono tem engate).
body.box(M.trim, [0, 0.7, 1.865], [0.7, 0.1, 0.06]);
body.box(M.trim, [0, 0.42, 1.915], [1.0, 0.16, 0.05]);
body.box(M.plate, [0, 0.55, 1.94], [0.4, 0.13, 0.012]);
body.box(M.plate, [0, 0.6, -1.945], [0.4, 0.13, 0.012]);
body.box(M.trim, [0, 0.3, -1.99], [0.08, 0.06, 0.14]);
body.box(M.rim, [0, 0.36, -2.04], [0.05, 0.05, 0.05]);

/**
 * Porta do lado esquerdo (+X), coordenadas do mundo: chapa pintada embaixo + janela escura em cima.
 * [win] = [z da janela na cintura, z da janela no alto, altura do alto].
 */
function door(zFront, zRear, zRearTop, win, withMirror) {
  const m = new Mesh();
  const outer = (half) => (z, y, o) => [half(y, z) + 0.006 + o * 0.022, y, z];
  m.slab(M.paint, [[zFront, 0.3], [zFront, 0.99], [zRearTop, 0.99], [zRear, 0.3]], outer(bodyHalf), () => [1, 0, 0], true);
  m.slab(M.glass, [[win[0], 0.99], [win[1], win[2]], [zRearTop, win[2]], [zRearTop, 0.99]], outer(glassHalf), () => [1, 0, 0], true);
  if (withMirror) m.box(M.paint, [bodyHalf(1, zFront) + 0.075, 1.03, zFront - 0.14], [0.13, 0.09, 0.12]);
  return m;
}
// Porta dianteira: da dobradiça (z 0,78) até a coluna B; a janela acompanha o para-brisa.
const doorFront = door(0.78, -0.17, -0.17, [0.76, 0.27, 1.44], true);
const doorRear = door(-0.19, -0.92, -1.05, [-0.19, -0.19, 1.45], false);

const hoodY = (z) => table([[0.95, 0.98], [1.3, 0.9], [1.78, 0.8]], z);
const hood = new Mesh();
hood.slab(M.paint, [[-0.74, 0.97], [0.74, 0.97], [0.7, 1.76], [-0.7, 1.76]],
  (x, z, o) => [x, hoodY(z) + 0.012 + o * 0.018, z], () => [0, 1, 0]);

const rearZ = (y) => table([[0.55, -1.945], [0.75, -1.952], [1.04, -1.918], [1.4, -1.818], [1.47, -1.76]], y);
const tailgate = new Mesh();
tailgate.slab(M.paint, [[-0.56, 0.55], [0.56, 0.55], [0.58, 1.03], [-0.58, 1.03]],
  (x, y, o) => [x, y, rearZ(y) - 0.006 - o * 0.02], () => [0, 0, -1]);
tailgate.slab(M.glass, [[-0.58, 1.03], [0.58, 1.03], [0.5, 1.42], [-0.5, 1.42]],
  (x, y, o) => [x, y, rearZ(y) - 0.006 - o * 0.02], () => [0, 0, -1]);
tailgate.box(M.trim, [0, 1.12, rearZ(1.12) - 0.04], [0.04, 0.03, 0.04]); // limpador

const wheel = (left) => {
  const m = new Mesh();
  m.cylinderX(M.tire, WHEEL_R, WHEEL_W, 28);
  const s = left ? 1 : -1;
  m.cylinderX(M.rim, 0.2, 0.012, 20, s > 0 ? WHEEL_W / 2 : -WHEEL_W / 2 - 0.012);
  return m;
};

// ---------- cena ----------
const nodes = [];
const meshes = [];
function node(name, mesh, translation) {
  const n = { name };
  if (translation) n.translation = translation;
  if (mesh) { n.mesh = meshes.length; meshes.push({ name, mesh }); }
  nodes.push(n);
  return nodes.length - 1;
}

const children = [];
children.push(node("body", body));
const hinge = (x, z) => [x, 0, z];
for (const [name, mesh, z, left] of [
  ["door_fl", doorFront, 0.78, true], ["door_fr", doorFront, 0.78, false],
  ["door_rl", doorRear, -0.19, true], ["door_rr", doorRear, -0.19, false],
]) {
  const h = hinge(bodyHalf(0.6, z) + 0.017, z);
  const local = mesh.relativeTo(h);
  children.push(left ? node(name, local, h) : node(name, local.mirrorX(), [-h[0], h[1], h[2]]));
}
const hoodHinge = [0, 0.98, 0.95];
children.push(node("hood", hood.relativeTo(hoodHinge), hoodHinge));
const gateHinge = [0, 1.47, -1.76];
children.push(node("tailgate", tailgate.relativeTo(gateHinge), gateHinge));
for (const [name, z, left] of [["wheel_fl", FRONT_AXLE, true], ["wheel_fr", FRONT_AXLE, false], ["wheel_rl", REAR_AXLE, true], ["wheel_rr", REAR_AXLE, false]]) {
  children.push(node(name, wheel(left), [left ? TRACK_HALF : -TRACK_HALF, WHEEL_R, z]));
}
// Lanternas: nós próprios (o app acende pelo nome). Esquerda = +X.
const lamp = (name, mat, c, s) => { const m = new Mesh(); m.box(mat, [0, 0, 0], s); children.push(node(name, m, c)); };
for (const [side, s] of [["l", 1], ["r", -1]]) {
  lamp(`lamp_head_${side}`, M.lamp_head, [s * 0.5, 0.76, 1.82], [0.32, 0.11, 0.1]);
  lamp(`lamp_turn_f${side}`, M.lamp_turn, [s * 0.69, 0.75, 1.78], [0.07, 0.08, 0.1]);
  lamp(`lamp_tail_${side}`, M.lamp_tail, [s * 0.66, 0.93, -1.93], [0.13, 0.22, 0.06]);
  lamp(`lamp_turn_r${side}`, M.lamp_turn, [s * 0.66, 0.78, -1.935], [0.13, 0.07, 0.05]);
}
const root = { name: "fox", children: [...children] };
nodes.push(root);
const rootIndex = nodes.length - 1;

// ---------- GLB ----------
const chunks = [];
let offset = 0;
const bufferViews = [];
const accessors = [];
function addView(typed, target) {
  const bytes = Buffer.from(typed.buffer, typed.byteOffset, typed.byteLength);
  const pad = (4 - (bytes.length % 4)) % 4;
  chunks.push(bytes, Buffer.alloc(pad));
  bufferViews.push({ buffer: 0, byteOffset: offset, byteLength: bytes.length, target });
  offset += bytes.length + pad;
  return bufferViews.length - 1;
}
function accessor(typed, type, componentType, count, extra = {}, target) {
  accessors.push({ bufferView: addView(typed, target), componentType, count, type, ...extra });
  return accessors.length - 1;
}

let triangles = 0;
const gltfMeshes = meshes.map(({ name, mesh }) => ({
  name,
  primitives: [...mesh.prims].map(([mat, p]) => {
    const count = p.pos.length / 3;
    if (count > 65535) throw new Error(`${name}: vértices demais para índice de 16 bits`);
    const min = [0, 1, 2].map((k) => Math.min(...p.pos.filter((_, i) => i % 3 === k)));
    const max = [0, 1, 2].map((k) => Math.max(...p.pos.filter((_, i) => i % 3 === k)));
    triangles += p.idx.length / 3;
    return {
      attributes: {
        POSITION: accessor(new Float32Array(p.pos), "VEC3", 5126, count, { min, max }, 34962),
        NORMAL: accessor(new Float32Array(p.nrm), "VEC3", 5126, count, {}, 34962),
      },
      indices: accessor(new Uint16Array(p.idx), "SCALAR", 5123, p.idx.length, {}, 34963),
      material: mat,
    };
  }),
}));

const gltf = {
  asset: { version: "2.0", generator: "JancarLauncher make-placeholder-fox", extras: { placeholder: true } },
  scene: 0,
  scenes: [{ nodes: [rootIndex] }],
  nodes,
  meshes: gltfMeshes,
  materials: MATERIALS.map(({ name, baseColorFactor, metallicFactor, roughnessFactor }) => ({
    name, pbrMetallicRoughness: { baseColorFactor, metallicFactor, roughnessFactor },
  })),
  accessors,
  bufferViews,
  buffers: [{ byteLength: offset }],
};

const bin = Buffer.concat(chunks);
let json = Buffer.from(JSON.stringify(gltf), "utf8");
json = Buffer.concat([json, Buffer.alloc((4 - (json.length % 4)) % 4, 0x20)]);
const header = Buffer.alloc(12);
header.writeUInt32LE(0x46546c67, 0);
header.writeUInt32LE(2, 4);
header.writeUInt32LE(12 + 8 + json.length + 8 + bin.length, 8);
const chunkHeader = (length, type) => { const b = Buffer.alloc(8); b.writeUInt32LE(length, 0); b.writeUInt32LE(type, 4); return b; };
const glb = Buffer.concat([header, chunkHeader(json.length, 0x4e4f534a), json, chunkHeader(bin.length, 0x004e4942), bin]);

mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, glb);
console.log(`${out}: ${(glb.length / 1024).toFixed(1)} KB, ${triangles} triângulos, ${gltfMeshes.length} malhas`);
