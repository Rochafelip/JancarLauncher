// Gera o modelo PROVISÓRIO do Fox (app/src/main/assets/car/fox.glb) até chegar o modelo comprado.
// Maquete low-poly com o perfil traçado da foto lateral do Fox do dono, nas medidas do Fox G2
// (3,823 × 1,640 × 1,544 m, entre-eixos 2,465 m), com as peças
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
// Cores conferidas nas fotos do carro (docs/Fox-photos/, 2026-11-13): prata, película bem escura, plásticos
// pretos, faróis com máscara escura. O app reconhece a pintura pelo nome "paint" (ganha verniz).
const MATERIALS = [
  { name: "paint", baseColorFactor: [0.7, 0.72, 0.75, 1], metallicFactor: 0.6, roughnessFactor: 0.32 },
  { name: "glass", baseColorFactor: [0.02, 0.024, 0.03, 1], metallicFactor: 0.0, roughnessFactor: 0.05 },
  { name: "trim", baseColorFactor: [0.035, 0.035, 0.04, 1], metallicFactor: 0.0, roughnessFactor: 0.6 },
  { name: "tire", baseColorFactor: [0.05, 0.05, 0.05, 1], metallicFactor: 0.0, roughnessFactor: 0.9 },
  { name: "rim", baseColorFactor: [0.74, 0.76, 0.78, 1], metallicFactor: 0.9, roughnessFactor: 0.28 },
  { name: "chrome", baseColorFactor: [0.85, 0.86, 0.88, 1], metallicFactor: 1.0, roughnessFactor: 0.12 },
  { name: "lamp_head", baseColorFactor: [0.26, 0.28, 0.31, 1], metallicFactor: 0.4, roughnessFactor: 0.12 },
  { name: "lamp_tail", baseColorFactor: [0.45, 0.04, 0.05, 1], metallicFactor: 0.0, roughnessFactor: 0.2 },
  { name: "lamp_turn", baseColorFactor: [0.55, 0.33, 0.05, 1], metallicFactor: 0.0, roughnessFactor: 0.2 },
  { name: "lamp_fog", baseColorFactor: [0.55, 0.57, 0.6, 1], metallicFactor: 0.3, roughnessFactor: 0.15 },
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
  /** Triângulo com normal por vértice (superfície lisa, sem facetas). */
  triN(mat, a, b, c, na, nb, nc) {
    const f = cross(sub(b, a), sub(c, a));
    if (len(f) < 1e-12) return;
    if (dot(f, add(add(na, nb), nc)) < 0) { [b, c] = [c, b]; [nb, nc] = [nc, nb]; }
    const p = this.prim(mat);
    const base = p.pos.length / 3;
    for (const [v, n] of [[a, na], [b, nb], [c, nc]]) { p.pos.push(...v); p.nrm.push(...norm(n)); }
    p.idx.push(base, base + 1, base + 2);
  }
  /** Disco/anel no plano X = [x] (roda), virado para [s] (±1), entre os raios [r0] e [r1]. */
  ringX(mat, r0, r1, x, s, seg) {
    for (let i = 0; i < seg; i++) {
      const a0 = (i / seg) * 2 * Math.PI, a1 = ((i + 1) / seg) * 2 * Math.PI;
      const p = (r, a) => [x, r * Math.sin(a), r * Math.cos(a)];
      if (r0 <= 0) this.tri(mat, [x, 0, 0], p(r1, a0), p(r1, a1), [s, 0, 0]);
      else this.quad(mat, p(r0, a0), p(r1, a0), p(r1, a1), p(r0, a1), [s, 0, 0]);
    }
  }
  /** Disco no plano Z = [z] (emblema, lente), virado para [s] (±1). */
  discZ(mat, [cx, cy], r, z, s, seg = 16) {
    for (let i = 0; i < seg; i++) {
      const a0 = (i / seg) * 2 * Math.PI, a1 = ((i + 1) / seg) * 2 * Math.PI;
      this.tri(mat, [cx, cy, z], [cx + r * Math.cos(a0), cy + r * Math.sin(a0), z], [cx + r * Math.cos(a1), cy + r * Math.sin(a1), z], [0, 0, s]);
    }
  }
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
  /** Cilindro deitado no eixo X (pneu), centrado na origem; lateral lisa; tampas opcionais. */
  cylinderX(mat, r, w, seg, x0 = -w / 2, caps = true) {
    const x1 = x0 + w;
    for (let i = 0; i < seg; i++) {
      const a0 = (i / seg) * 2 * Math.PI, a1 = ((i + 1) / seg) * 2 * Math.PI;
      const p = (x, a) => [x, r * Math.sin(a), r * Math.cos(a)];
      const n = (a) => [0, Math.sin(a), Math.cos(a)];
      this.triN(mat, p(x0, a0), p(x1, a0), p(x1, a1), n(a0), n(a0), n(a1));
      this.triN(mat, p(x0, a0), p(x1, a1), p(x0, a1), n(a0), n(a1), n(a1));
      if (!caps) continue;
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
// Perfil TRAÇADO da foto lateral do carro do dono (docs/Fox-photos/IMG_20261113_150109828_HDR_PCT.jpg.jpeg,
// lado direito, frente à direita da foto). Escala: na horizontal, os centros das rodas (entre-eixos real
// 2,465 m) e o comprimento total (3,823 m) repartido nos balanços como na foto; na vertical, o teto em 1,544 m
// com o chão na base dos pneus. Largura e cantos pela foto de frente e pela traseira 3/4.
const FRONT_AXLE = 1.165, REAR_AXLE = -1.3; // entre-eixos 2,465
const TRACK_HALF = 0.70, WHEEL_R = 0.272, WHEEL_W = 0.19, ARCH_R = 0.315, RIM_R = 0.205;
/** Cantos arredondados vistos de cima: a frente arredonda mais que a traseira (fotos de frente/traseira). */
const taper = (z) => (z > 0 ? 1 - 0.11 * smooth(1.35, 1.97, z) : 1 - 0.07 * smooth(1.45, 1.86, -z));
const bodyHalf = (y, z) => (y < 0.75 ? 0.8 : 0.8 - (y - 0.75) * 0.12) * taper(z);
const glassHalf = (y, z) => (0.765 - (y - 0.95) * 0.3) * taper(z);

function arch(zc, from, to, steps = 10) {
  const pts = [];
  for (let i = 0; i <= steps; i++) {
    const a = ((from + (to - from) * (i / steps)) * Math.PI) / 180;
    pts.push([zc + ARCH_R * Math.cos(a), WHEEL_R + ARCH_R * Math.sin(a)]);
  }
  return pts;
}

/** Linha de cintura (base dos vidros), descendo para a frente como no Fox: (z, y). */
const BELT = [[0.905, 0.99], [0.546, 0.93], [-0.26, 0.978], [-0.42, 1.0], [-1.246, 1.063], [-1.748, 1.067]];
const beltY = (z) => table([...BELT].reverse(), z);

// Perfil lateral (z, y) da carroceria abaixo da cintura, com capô, para-choques e caixas de roda.
const bodyProfile = chaikin([
  [1.859, 0.235], [1.941, 0.364], [1.965, 0.549], [1.941, 0.697], [1.883, 0.789], [1.777, 0.845],
  [1.53, 0.919], [1.294, 0.958], [1.013, 0.985], ...BELT,
  [-1.785, 1.011], [-1.823, 0.919], [-1.84, 0.789], [-1.86, 0.66], [-1.86, 0.475], [-1.823, 0.364],
  [-1.76, 0.281], [-1.7, 0.245],
  ...arch(REAR_AXLE, 190, -10), ...arch(FRONT_AXLE, 190, -10), [1.65, 0.235],
], 2);
// Estufa (vidros) acima da cintura: para-brisa, teto, vigia traseira.
const ROOF = [[0.26, 1.473], [0.009, 1.5], [-0.529, 1.535], [-0.888, 1.53], [-1.157, 1.5], [-1.4, 1.473]];
const glassProfile = chaikin([
  [0.905, 0.985], ...ROOF, [-1.462, 1.455], [-1.599, 1.27], [-1.698, 1.14], [-1.748, 1.06],
  ...[...BELT].reverse().slice(1).map(([z, y]) => [z, y - 0.01]),
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
  // Anti-horário em (z, y): a normal de fora da borda a→b é (dy, -dz) em (z, y).
  const edgeN = ring.map((a, i) => {
    const b = ring[(i + 1) % ring.length];
    return norm([0, -(b[0] - a[0]), b[1] - a[1]]);
  });
  // Normal por vértice: média das duas bordas quando a dobra é suave (teto, capô, para-choques ficam lisos);
  // dobra forte (> ~37°, ex.: caixa de roda) continua marcada.
  const vertexN = (i, own) => {
    const prev = edgeN[(i + ring.length - 1) % ring.length], next = edgeN[i % ring.length];
    return dot(prev, next) > 0.8 ? norm(add(prev, next)) : own;
  };
  for (let i = 0; i < ring.length; i++) {
    const j = (i + 1) % ring.length;
    const a = ring[i], b = ring[j];
    const pa = (s) => [s * half(a[1], a[0]), a[1], a[0]];
    const pb = (s) => [s * half(b[1], b[0]), b[1], b[0]];
    const na = vertexN(i, edgeN[i]), nb = vertexN(j, edgeN[i]);
    mesh.triN(mat, pa(1), pb(1), pb(-1), na, nb, nb);
    mesh.triN(mat, pa(1), pb(-1), pa(-1), na, nb, na);
  }
}

// ---------- peças ----------
const body = new Mesh();
extrude(body, M.paint, bodyProfile, bodyHalf);
extrude(body, M.glass, glassProfile, glassHalf);
// Teto pintado por cima do vidro: vai até o alto das janelas (1,44–1,455 m na foto).
extrude(body, M.paint, chaikin([
  [0.24, 1.466], ...ROOF.slice(1).map(([z, y]) => [z, y + 0.006]), [-1.45, 1.462],
  [-1.4, 1.44], [-0.977, 1.455], [-0.422, 1.455], [-0.26, 1.452], [0.188, 1.442],
], 1), (y, z) => glassHalf(y, z) + 0.003);
for (const s of [1, -1]) {
  const onGlass = (extra) => (z, y, o) => [s * (glassHalf(y, z) + extra + o * 0.012), y, z];
  // Coluna A (cor do carro) acompanhando o para-brisa.
  body.slab(M.paint, [[0.905, 0.985], [0.26, 1.473], [0.2, 1.452], [0.85, 0.99]], onGlass(0.004), () => [s, 0, 0], true);
  // Coluna C larga, pintada, entre o vidro fixo da porta traseira e a traseira.
  body.slab(M.paint, chaikin([[-1.192, 1.44], [-1.4, 1.475], [-1.462, 1.455], [-1.599, 1.27], [-1.698, 1.14],
    [-1.748, 1.06], [-1.264, 1.06], [-1.24, 1.3]], 1), onGlass(0.004), () => [s, 0, 0], true);
}
// Frente: grade preta com dois frisos cromados na altura dos faróis, emblema (disco genérico), colmeia
// embaixo, neblina redonda em moldura preta (apagada: não há sinal da neblina) e placa em branco.
body.box(M.trim, [0, 0.72, 1.925], [0.62, 0.1, 0.06]);
for (const y of [0.745, 0.7]) body.box(M.chrome, [0, y, 1.957], [0.62, 0.008, 0.006]);
body.discZ(M.chrome, [0, 0.722], 0.055, 1.962, 1, 20);
body.box(M.trim, [0, 0.42, 1.945], [1.0, 0.16, 0.04]);
for (const s of [1, -1]) {
  body.box(M.trim, [s * 0.6, 0.4, 1.925], [0.2, 0.11, 0.03]);
  body.discZ(M.lamp_fog, [s * 0.6, 0.4], 0.038, 1.942, 1);
}
body.box(M.plate, [0, 0.55, 1.972], [0.4, 0.13, 0.012]);
// Traseira: placa em branco, engate com bola (o Fox do dono tem), sensores de ré e refletores no para-choque.
body.box(M.plate, [0, 0.6, -1.866], [0.4, 0.13, 0.012]);
body.box(M.trim, [0, 0.3, -1.9], [0.08, 0.06, 0.14]);
body.box(M.chrome, [0, 0.36, -1.95], [0.05, 0.05, 0.05]);
for (const s of [1, -1]) {
  for (const x of [0.36, 0.56]) body.discZ(M.trim, [s * x, 0.5], 0.013, -1.868, -1, 10);
  body.box(M.lamp_tail, [s * 0.6, 0.36, -1.83], [0.13, 0.025, 0.01]);
}
// Teto: antena "barbatana" preta (na foto, sobre a porta traseira).
body.slab(M.trim, [[-0.84, 1.531], [-1.0, 1.531], [-0.96, 1.6], [-0.92, 1.605]],
  (z, y, o) => [(o - 0.5) * 0.05, y, z], () => [1, 0, 0]);
// Portinhola de combustível: lado direito (−X), entre a caixa de roda e a lanterna (foto lateral).
body.slab(M.paint, chaikin([[-1.44, 0.82], [-1.6, 0.82], [-1.6, 0.96], [-1.44, 0.96]], 2),
  (z, y, o) => [-(bodyHalf(y, z) + 0.002 + o * 0.004), y, z], () => [-1, 0, 0], true);

/**
 * Porta do lado esquerdo (+X), coordenadas do mundo, desenhada pela foto: [panel] = chapa (borda de cima na
 * cintura), [glass] = janela, [black] = plásticos pretos por cima do vidro (película escura + molduras).
 */
function door(opts) {
  const m = new Mesh();
  const outer = (half, extra = 0) => (z, y, o) => [half(y, z) + 0.006 + extra + o * 0.022, y, z];
  m.slab(M.paint, opts.panel, outer(bodyHalf), () => [1, 0, 0], true);
  m.slab(M.glass, opts.glass, outer(glassHalf), () => [1, 0, 0], true);
  for (const poly of opts.black) m.slab(M.trim, poly, outer(glassHalf, 0.003), () => [1, 0, 0], true);
  // Friso lateral fino na cor do carro e maçaneta na cor do carro.
  const [z0, z1] = opts.molding;
  m.slab(M.paint, [[z0, 0.44], [z1, 0.44], [z1, 0.46], [z0, 0.46]], outer(bodyHalf, 0.022), () => [1, 0, 0], true);
  const [hz, hy] = opts.handle;
  m.box(M.paint, [bodyHalf(hy, hz) + 0.035, hy, hz], [0.022, 0.03, 0.13]);
  if (opts.mirror) {
    const x = bodyHalf(1, 0.64);
    m.box(M.trim, [x + 0.03, 0.975, 0.7], [0.05, 0.05, 0.08]); // base
    m.box(M.paint, [x + 0.1, 0.995, 0.64], [0.13, 0.12, 0.18]); // concha na cor do carro
  }
  return m;
}
const FRONT_HINGE_Z = 0.771, REAR_HINGE_Z = -0.314;
const doorFront = door({
  panel: [[0.744, 0.262], [FRONT_HINGE_Z, beltY(FRONT_HINGE_Z)], [0.546, 0.93], [-0.287, beltY(-0.287)], [-0.287, 0.262]],
  glass: [[0.88, 0.988], [0.224, 1.436], [-0.26, 1.448], [-0.26, 0.975], [0.546, 0.928]],
  black: [
    [[0.905, 0.99], [0.72, 0.965], [0.737, 1.1]], // triângulo do retrovisor
    [[-0.2, 0.975], [-0.26, 0.976], [-0.26, 1.448], [-0.2, 1.446]], // coluna B
  ],
  molding: [0.7, -0.25], handle: [-0.171, 0.817], mirror: true,
});
const doorRear = door({
  panel: [[REAR_HINGE_Z, 0.262], [REAR_HINGE_Z, beltY(REAR_HINGE_Z)], [-1.246, 1.063], [-1.264, 1.063],
    [-1.32, 0.919], [-1.291, 0.771], [-1.21, 0.622], [-1.067, 0.492], [-0.941, 0.381], [-0.888, 0.272]],
  glass: [[REAR_HINGE_Z, beltY(REAR_HINGE_Z) - 0.01], [REAR_HINGE_Z, 1.455], [-0.977, 1.455], [-1.192, 1.43],
    [-1.24, 1.3], [-1.264, 1.055], [-0.42, 0.99]],
  black: [
    [[REAR_HINGE_Z, 0.995], [-0.36, 0.998], [-0.36, 1.455], [REAR_HINGE_Z, 1.455]], // coluna B
    [[-0.995, 1.03], [-1.022, 1.035], [-1.022, 1.455], [-0.995, 1.455]], // divisão do vidro fixo
  ],
  molding: [-0.33, -0.94], handle: [-1.148, 0.86],
});
/** Repetidor de seta embaixo do retrovisor (coordenadas do mundo, lado esquerdo). */
const mirrorRepeater = [bodyHalf(1, 0.64) + 0.14, 0.94, 0.6];

// Capô: da base do para-brisa até a grade, na altura do perfil.
const hoodY = (z) => table([[0.905, 0.99], [1.013, 0.985], [1.294, 0.958], [1.53, 0.919], [1.777, 0.845], [1.87, 0.8]], z);
const hood = new Mesh();
hood.slab(M.paint, [[-0.7, 0.95], [0.7, 0.95], [0.64, 1.86], [-0.64, 1.86]],
  (x, z, o) => [x, hoodY(z) + 0.008 + o * 0.016, z], () => [0, 1, 0]);

// Tampa traseira: chapa e vigia seguindo o caimento da traseira (perfil da foto).
const rearZ = (y) => table([[0.475, -1.86], [0.66, -1.86], [0.789, -1.84], [0.919, -1.823], [1.011, -1.785],
  [1.067, -1.748], [1.14, -1.698], [1.27, -1.599], [1.455, -1.462]], y);
const tailgate = new Mesh();
const onRear = (x, y, o) => [x, y, rearZ(y) - 0.006 - o * 0.02];
tailgate.slab(M.paint, [[-0.56, 0.66], [0.56, 0.66], [0.6, 1.06], [-0.6, 1.06]], onRear, () => [0, 0, -1]);
tailgate.slab(M.glass, [[-0.6, 1.06], [0.6, 1.06], [0.5, 1.43], [-0.5, 1.43]], onRear, () => [0, 0, -1]);
// Limpador traseiro: eixo + palheta deitada no vidro.
tailgate.box(M.trim, [0, 1.12, rearZ(1.12) - 0.04], [0.04, 0.03, 0.04]);
tailgate.box(M.trim, [0.17, 1.15, rearZ(1.15) - 0.03], [0.36, 0.012, 0.012]);
// Emblema (disco genérico) e a câmera de ré logo abaixo (confirmada pelo dono).
tailgate.discZ(M.chrome, [0, 0.93], 0.055, rearZ(0.93) - 0.028, -1, 20);
tailgate.discZ(M.trim, [0, 0.845], 0.014, rearZ(0.845) - 0.028, -1, 10);
// Aerofólio no fim do teto (preso na tampa, sobe junto) com a luz de freio alta, apagada: não há sinal do
// pedal de freio.
tailgate.slab(M.paint, chaikin([[-1.36, 1.5], [-1.42, 1.497], [-1.5, 1.455], [-1.515, 1.43], [-1.46, 1.44], [-1.38, 1.47]], 1),
  (z, y, o) => [(o - 0.5) * 1.18, y, z], () => [1, 0, 0]);
tailgate.box(M.lamp_tail, [0, 1.44, -1.49], [0.24, 0.02, 0.03]);

/** Roda de liga 15" do Fox do dono: 5 pares de raios em V, aro prata, miolo escuro, calota central. */
const wheel = (left) => {
  const m = new Mesh();
  const s = left ? 1 : -1;
  const face = s * WHEEL_W / 2;
  m.cylinderX(M.tire, WHEEL_R, WHEEL_W, 32, -WHEEL_W / 2, false);
  m.ringX(M.tire, RIM_R, WHEEL_R, face, s, 32); // flanco de fora
  m.ringX(M.tire, 0, WHEEL_R, -face, -s, 24); // lado de dentro fechado
  m.ringX(M.trim, 0, RIM_R, face - s * 0.05, s, 24); // miolo escuro (aparece entre os raios)
  m.cylinderX(M.rim, RIM_R, 0.05, 32, s > 0 ? face - 0.05 : face, false); // tala
  m.ringX(M.rim, RIM_R - 0.02, RIM_R + 0.004, face + s * 0.004, s, 32); // borda do aro
  for (let k = 0; k < 5; k++) {
    const a = (k / 5) * 2 * Math.PI;
    for (const d of [-1, 1]) {
      const pt = (r, ang, w) => {
        const c = [r * Math.sin(ang), r * Math.cos(ang)];
        const t = [Math.cos(ang), -Math.sin(ang)]; // perpendicular no plano da roda
        return [[c[0] + t[0] * w, c[1] + t[1] * w], [c[0] - t[0] * w, c[1] - t[1] * w]];
      };
      const [i0, i1] = pt(0.055, a + d * 0.07, 0.013);
      const [o0, o1] = pt(RIM_R - 0.012, a + d * 0.2, 0.009);
      m.slab(M.rim, [i0, o0, o1, i1], (y, z, o) => [face - s * 0.045 + s * o * 0.048, y, z], () => [s, 0, 0]);
    }
  }
  m.cylinderX(M.rim, 0.06, 0.02, 20, s > 0 ? face - 0.015 : face - 0.005); // cubo
  m.ringX(M.chrome, 0, 0.032, face + s * 0.0055, s, 16); // calota central
  return m;
};

// ---------- cena ----------
const nodes = [];
const meshes = [];
function node(name, mesh, translation, kids) {
  const n = { name };
  if (translation) n.translation = translation;
  if (mesh) { n.mesh = meshes.length; meshes.push({ name, mesh }); }
  if (kids?.length) n.children = kids;
  nodes.push(n);
  return nodes.length - 1;
}
const boxMesh = (mat, size) => { const m = new Mesh(); m.box(mat, [0, 0, 0], size); return m; };

const children = [];
children.push(node("body", body));
const hinge = (x, z) => [x, 0, z];
for (const [name, mesh, z, left] of [
  ["door_fl", doorFront, FRONT_HINGE_Z, true], ["door_fr", doorFront, FRONT_HINGE_Z, false],
  ["door_rl", doorRear, REAR_HINGE_Z, true], ["door_rr", doorRear, REAR_HINGE_Z, false],
]) {
  const h = hinge(bodyHalf(0.6, z) + 0.017, z);
  const local = mesh.relativeTo(h);
  // Repetidor de seta do retrovisor: nó próprio (o app pisca pelo nome), filho da porta (abre junto).
  const kids = [];
  if (mesh === doorFront) {
    const rel = sub(mirrorRepeater, h);
    kids.push(node(left ? "lamp_turn_ml" : "lamp_turn_mr", boxMesh(M.lamp_turn, [0.06, 0.014, 0.05]), left ? rel : [-rel[0], rel[1], rel[2]]));
  }
  children.push(left ? node(name, local, h, kids) : node(name, local.mirrorX(), [-h[0], h[1], h[2]], kids));
}
const hoodHinge = [0, 0.99, 0.95];
children.push(node("hood", hood.relativeTo(hoodHinge), hoodHinge));
const gateHinge = [0, 1.46, -1.44];
children.push(node("tailgate", tailgate.relativeTo(gateHinge), gateHinge));
for (const [name, z, left] of [["wheel_fl", FRONT_AXLE, true], ["wheel_fr", FRONT_AXLE, false], ["wheel_rl", REAR_AXLE, true], ["wheel_rr", REAR_AXLE, false]]) {
  children.push(node(name, wheel(left), [left ? TRACK_HALF : -TRACK_HALF, WHEEL_R, z]));
}
// Lanternas: nós próprios (o app acende pelo nome). Esquerda = +X.
const lamp = (name, mat, c, s) => children.push(node(name, boxMesh(mat, s), c));
for (const [side, s] of [["l", 1], ["r", -1]]) {
  // Farol com máscara escura e a seta âmbar numa faixa embaixo dele (como nas fotos).
  lamp(`lamp_head_${side}`, M.lamp_head, [s * 0.56, 0.735, 1.875], [0.3, 0.13, 0.12]);
  lamp(`lamp_turn_f${side}`, M.lamp_turn, [s * 0.56, 0.66, 1.9], [0.24, 0.025, 0.08]);
  // Lanterna G2 dobrando a quina (vermelho por fora) e a seta na parte de dentro, mais baixa.
  lamp(`lamp_tail_${side}`, M.lamp_tail, [s * 0.645, 0.915, -1.79], [0.15, 0.24, 0.14]);
  lamp(`lamp_turn_r${side}`, M.lamp_turn, [s * 0.53, 0.86, -1.825], [0.09, 0.07, 0.05]);
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
