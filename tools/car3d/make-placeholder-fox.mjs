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
  { name: "interior", baseColorFactor: [0.07, 0.07, 0.075, 1], metallicFactor: 0.0, roughnessFactor: 0.85 },
];
const M = Object.fromEntries(MATERIALS.map((m, i) => [m.name, i]));

// ---------- malha com normais por face ----------
class Mesh {
  constructor() { this.prims = new Map(); }
  prim(mat) {
    if (!this.prims.has(mat)) this.prims.set(mat, { pos: [], nrm: [], idx: [], keys: new Map() });
    return this.prims.get(mat);
  }
  /** Índice do vértice (posição + normal); vértices iguais são compartilhados (menos memória na central). */
  vert(p, v, n) {
    const key = v.map((c) => c.toFixed(5)).join(",") + "|" + n.map((c) => c.toFixed(3)).join(",");
    let i = p.keys.get(key);
    if (i === undefined) {
      i = p.pos.length / 3;
      p.pos.push(...v); p.nrm.push(...n);
      p.keys.set(key, i);
    }
    return i;
  }
  /** Triângulo com normal da face; [hint] = direção "para fora" (acerta a ordem dos vértices). */
  /** [exact] = usa [hint] como normal (lateral levemente curva sem vincos de triangulação). */
  tri(mat, a, b, c, hint, exact = false) {
    let n = cross(sub(b, a), sub(c, a));
    if (len(n) < 1e-12) return;
    if (hint && dot(n, hint) < 0) { [b, c] = [c, b]; n = scale(n, -1); }
    n = exact && hint ? norm(hint) : norm(n);
    const p = this.prim(mat);
    p.idx.push(this.vert(p, a, n), this.vert(p, b, n), this.vert(p, c, n));
  }
  quad(mat, a, b, c, d, hint) { this.tri(mat, a, b, c, hint); this.tri(mat, a, c, d, hint); }
  /** Triângulo com normal por vértice (superfície lisa, sem facetas). */
  triN(mat, a, b, c, na, nb, nc) {
    const f = cross(sub(b, a), sub(c, a));
    if (len(f) < 1e-12) return;
    if (dot(f, add(add(na, nb), nc)) < 0) { [b, c] = [c, b]; [nb, nc] = [nc, nb]; }
    const p = this.prim(mat);
    p.idx.push(this.vert(p, a, norm(na)), this.vert(p, b, norm(nb)), this.vert(p, c, norm(nc)));
  }
  /**
   * Superfície de revolução em torno do eixo X (pneu com ombro redondo): [profile] = [[x, raio], ...] aberto,
   * normais lisas pela derivada do perfil.
   */
  revolveX(mat, profile, seg) {
    const n2 = profile.map((p, i) => {
      const a = profile[Math.max(0, i - 1)], b = profile[Math.min(profile.length - 1, i + 1)];
      return norm([b[1] - a[1], -(b[0] - a[0])]); // (dx, dr) girado: normal no plano (x, r)
    });
    const P = (i, a) => [profile[i][0], profile[i][1] * Math.sin(a), profile[i][1] * Math.cos(a)];
    const Nn = (i, a) => [n2[i][0], n2[i][1] * Math.sin(a), n2[i][1] * Math.cos(a)];
    for (let k = 0; k < seg; k++) {
      const a0 = (k / seg) * 2 * Math.PI, a1 = ((k + 1) / seg) * 2 * Math.PI;
      for (let i = 0; i + 1 < profile.length; i++) {
        this.triN(mat, P(i, a0), P(i + 1, a0), P(i + 1, a1), Nn(i, a0), Nn(i + 1, a0), Nn(i + 1, a1));
        this.triN(mat, P(i, a0), P(i + 1, a1), P(i, a1), Nn(i, a0), Nn(i + 1, a1), Nn(i, a1));
      }
    }
  }
  /**
   * Chapa em grade densa: [f](u, v, o) com u, v em 0..1 e o = 1 (fora) / 0 (dentro). Para superfícies com
   * detalhe fino (vincos do capô), que a subdivisão de triângulos não pega. [outward] = direção de fora.
   */
  gridPatch(mat, nu, nv, f, outward, edgeMat = mat, back = [nu, nv]) {
    const e = 1e-3;
    const nrm = (u, v, o) => {
      const du = sub(f(Math.min(1, u + e), v, o), f(Math.max(0, u - e), v, o));
      const dv = sub(f(u, Math.min(1, v + e), o), f(u, Math.max(0, v - e), o));
      let n = norm(cross(du, dv));
      if (dot(n, outward) < 0) n = scale(n, -1);
      return o === 1 ? n : scale(n, -1);
    };
    // Face de fora na resolução cheia; a de dentro ([back]) pode ser bem mais simples quando quase não aparece.
    for (const o of [1, 0]) {
      const [gu, gv] = o === 1 ? [nu, nv] : back;
      for (let i = 0; i < gu; i++) {
        for (let j = 0; j < gv; j++) {
          const q = [[i / gu, j / gv], [(i + 1) / gu, j / gv], [(i + 1) / gu, (j + 1) / gv], [i / gu, (j + 1) / gv]];
          const P = q.map(([u, v]) => f(u, v, o)), N = q.map(([u, v]) => nrm(u, v, o));
          this.triN(mat, P[0], P[1], P[2], N[0], N[1], N[2]);
          this.triN(mat, P[0], P[2], P[3], N[0], N[2], N[3]);
        }
      }
    }
    // Borda (espessura) em volta.
    const edge = [];
    for (let i = 0; i < nu; i++) edge.push([i / nu, 0]);
    for (let j = 0; j < nv; j++) edge.push([1, j / nv]);
    for (let i = nu; i > 0; i--) edge.push([i / nu, 1]);
    for (let j = nv; j > 0; j--) edge.push([0, j / nv]);
    const mid = f(0.5, 0.5, 0.5);
    for (let k = 0; k < edge.length; k++) {
      const a = edge[k], b = edge[(k + 1) % edge.length];
      const pa1 = f(...a, 1), pb1 = f(...b, 1), pa0 = f(...a, 0), pb0 = f(...b, 0);
      this.quad(edgeMat, pa0, pb0, pb1, pa1, sub(scale(add(pa1, pb1), 0.5), mid));
    }
  }
  /** Superelipsoide (peça arredondada: concha do retrovisor), centro [c], semi-eixos [rad], expoente [p]. */
  blob(mat, c, rad, p = 2.6, nu = 20, nv = 12) {
    const f = (w, e) => Math.sign(w) * Math.pow(Math.abs(w), e);
    const e = 2 / p;
    const P = (u, v) => [
      c[0] + rad[0] * f(Math.cos(v), e) * f(Math.cos(u), e),
      c[1] + rad[1] * f(Math.sin(v), e),
      c[2] + rad[2] * f(Math.cos(v), e) * f(Math.sin(u), e),
    ];
    // Normal da superelipsoide: gradiente da função implícita.
    const Nn = (q) => {
      const g = [0, 1, 2].map((k) => { const t = (q[k] - c[k]) / rad[k]; return (p * Math.sign(t) * Math.pow(Math.abs(t), p - 1)) / rad[k]; });
      return len(g) < 1e-9 ? [0, 1, 0] : g;
    };
    for (let i = 0; i < nu; i++) {
      for (let j = 0; j < nv; j++) {
        const u0 = (i / nu) * 2 * Math.PI, u1 = ((i + 1) / nu) * 2 * Math.PI;
        const v0 = -Math.PI / 2 + (j / nv) * Math.PI, v1 = -Math.PI / 2 + ((j + 1) / nv) * Math.PI;
        const [a, b, cc, d] = [P(u0, v0), P(u1, v0), P(u1, v1), P(u0, v1)];
        this.triN(mat, a, b, cc, Nn(a), Nn(b), Nn(cc));
        this.triN(mat, a, cc, d, Nn(a), Nn(cc), Nn(d));
      }
    }
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
  slab(mat, poly, map, outward, exactCaps = false, subdiv = 1, edgeMat = mat) {
    const refine = (r, n) => (n <= 1 ? r : r.flatMap((a, i) => {
      const b = r[(i + 1) % r.length];
      return Array.from({ length: n }, (_, k) => [a[0] + ((b[0] - a[0]) * k) / n, a[1] + ((b[1] - a[1]) * k) / n]);
    }));
    // Chapa curva: a borda é dividida (senão liga só os cantos e abre frestas na curva), e a faixa da borda
    // usa os mesmos pontos que a face usa nela (face subdivide cada trecho em [capN]); senão a face curva
    // tapa a faixa em pedaços alternados e a borda aparece tracejada.
    const capN = subdiv > 1 ? Math.max(1, Math.ceil(subdiv / 3)) : 1;
    const ring = refine(ccw(poly), subdiv);
    for (const [i, j, k] of earclip(ring)) {
      const [a, b, c] = [ring[i], ring[j], ring[k]];
      if (subdiv > 1) { this.curvedCaps(mat, a, b, c, map, outward, capN); continue; }
      this.tri(mat, map(...a, 1), map(...b, 1), map(...c, 1), outward(...a), exactCaps);
      this.tri(mat, map(...a, 0), map(...b, 0), map(...c, 0), scale(outward(...a), -1), exactCaps);
    }
    const edgeRing = refine(ring, capN);
    for (let i = 0; i < edgeRing.length; i++) {
      const a = edgeRing[i], b = edgeRing[(i + 1) % edgeRing.length];
      // Normal da borda no plano 2D (polígono anti-horário: a de fora fica à direita).
      const e = [b[0] - a[0], b[1] - a[1]];
      const n2 = [e[1], -e[0]];
      const pa0 = map(...a, 0), pa1 = map(...a, 1), pb0 = map(...b, 0), pb1 = map(...b, 1);
      const mid = scale(add(add(pa0, pb1), add(pa1, pb0)), 0.25);
      const out2 = sub(map(a[0] + n2[0] * 0.01, a[1] + n2[1] * 0.01, 0.5), map(...a, 0.5));
      this.quad(edgeMat, pa0, pb0, pb1, pa1, len(out2) > 0 ? out2 : mid);
    }
  }
  /**
   * Faces de fora/de dentro de uma chapa curva: o triângulo (a, b, c) do polígono é dividido em n² pedaços e
   * cada vértice passa por [map], então a chapa acompanha a curva da lataria; normais pela derivada de [map].
   */
  /**
   * Decalque: só a face de fora de uma chapa colada (colmeia, vão escuro, forro), sem verso nem borda, que
   * nunca aparecem — economiza triângulos na central.
   */
  decal(mat, poly, map, outward, subdiv = 1) {
    const ring = ccw(poly);
    for (const [i, j, k] of earclip(ring)) {
      const [a, b, c] = [ring[i], ring[j], ring[k]];
      if (subdiv > 1) this.curvedCaps(mat, a, b, c, map, outward, subdiv, [1]);
      else this.tri(mat, map(...a, 1), map(...b, 1), map(...c, 1), outward(...a));
    }
  }
  curvedCaps(mat, a, b, c, map, outward, n, sides = [1, 0]) {
    const P = (i, j) => { // ponto barycêntrico (i, j) da grade do triângulo
      const u = i / n, v = j / n, w = 1 - u - v;
      return [a[0] * w + b[0] * u + c[0] * v, a[1] * w + b[1] * u + c[1] * v];
    };
    const e = 1e-3;
    const nrm = (q, o) => {
      const du = sub(map(q[0] + e, q[1], o), map(q[0] - e, q[1], o));
      const dv = sub(map(q[0], q[1] + e, o), map(q[0], q[1] - e, o));
      let nn = norm(cross(du, dv));
      if (dot(nn, outward(...q)) < 0) nn = scale(nn, -1);
      return o === 1 ? nn : scale(nn, -1);
    };
    const put = (q1, q2, q3) => {
      for (const o of sides) this.triN(mat, map(...q1, o), map(...q2, o), map(...q3, o), nrm(q1, o), nrm(q2, o), nrm(q3, o));
    };
    for (let i = 0; i < n; i++) {
      for (let j = 0; j < n - i; j++) {
        put(P(i, j), P(i + 1, j), P(i, j + 1));
        if (i + j < n - 1) put(P(i + 1, j), P(i + 1, j + 1), P(i, j + 1));
      }
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
const taper = (z) => (z > 0 ? 1 - 0.13 * smooth(1.2, 1.97, z) : 1 - 0.09 * smooth(1.35, 1.86, -z));
/** Meia-largura da lataria: barriga no meio da porta (y ≈ 0,55), recolhe na soleira e fecha acima da cintura. */
const sideHalf = (y) => (y < 0.55
  ? 0.8 - 0.035 * ((0.55 - y) / 0.3) ** 2
  : 0.8 - 0.01 * ((Math.min(y, 0.75) - 0.55) / 0.2) ** 2 - (y > 0.75 ? 0.12 * (y - 0.75) : 0));
const bodyHalf = (y, z) => sideHalf(y) * taper(z);
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

/** Expoente da seção (superelipse): 3 = quinas bem arredondadas mas laterais ainda "de carro", retas. */
const SECTION_P = 2.6;
/**
 * Quanto a superfície sobe (ou avança) em relação ao perfil na posição [x] da largura: [crown] no meio,
 * ~0 perto da borda e −[r] na borda (onde vira o ombro). Mesmo desenho de [extrudeRound]; usado para capô,
 * tampa e peças coladas na frente/traseira seguirem a curva.
 */
function bulge(x, half, crown, r) {
  const t = Math.min(1, Math.abs(x) / half);
  const s = Math.pow(t, SECTION_P / 2);
  const c = Math.sqrt(Math.max(0, 1 - s * s));
  return (crown + r) * Math.pow(c, 2 / SECTION_P) - r;
}

/**
 * Extrusão arredondada do perfil (z, y): a seção na largura é uma superelipse, então teto, capô e
 * para-choques ficam abaulados ([crownTop]/[crownEnd] no meio) e viram para a lateral num ombro de raio
 * [r]; a lateral (tampa) fica em x = ±[half](y, z), com o perfil recuado [r]. [matOf](z, y, normal2D) escolhe
 * o material de cada faixa (ex.: teto pintado sobre a estufa de vidro).
 */
function extrudeRound(mesh, matOf, capMat, profile, half, { r, crownTop, crownEnd, steps = 18, capSubdiv = 5 }) {
  const ring = ccw(profile);
  const N = ring.length;
  // Normais 2D (z, y) de cada borda e de cada vértice (bissetriz); dobra forte = aresta viva no sombreado.
  const en = ring.map((a, i) => { const b = ring[(i + 1) % N]; return norm([b[1] - a[1], -(b[0] - a[0])]); });
  const vn = ring.map((_, i) => norm(add(en[(i + N - 1) % N], en[i])));
  const sharp = ring.map((_, i) => dot(en[(i + N - 1) % N], en[i]) < 0.8);
  const crown = vn.map(([nz, ny]) => crownTop * Math.max(0, ny) ** 2 + crownEnd * nz * nz);
  const e = 2 / SECTION_P;
  // Perfil recuado [r]: é onde a lateral começa; a largura é medida ali, para a borda bater com a lateral.
  const inset = ring.map(([z, y], i) => [z - vn[i][0] * r, y - vn[i][1] * r]);
  const hw = inset.map(([z, y]) => half(y, z));
  const G = (i, k) => {
    const th = -Math.PI / 2 + (Math.PI * k) / steps;
    const s = Math.sin(th), c = Math.cos(th);
    const [z, y] = ring[(i + N) % N], [nz, ny] = vn[(i + N) % N];
    const x = Math.sign(s) * Math.pow(Math.abs(s), e) * hw[(i + N) % N];
    const d = (crown[(i + N) % N] + r) * Math.pow(Math.abs(c), e) - r;
    return [x, y + ny * d, z + nz * d];
  };
  // Normal no vértice (i, k); [toward] = vizinho do lado da faixa (nas dobras fortes não mistura os lados).
  const normalAt = (i, k, toward) => {
    const ti = sharp[i]
      ? (toward > 0 ? sub(G(i + 1, k), G(i, k)) : sub(G(i, k), G(i - 1, k)))
      : sub(G(i + 1, k), G(i - 1, k));
    const tk = sub(G(i, Math.min(steps, k + 1)), G(i, Math.max(0, k - 1)));
    let n = norm(cross(ti, tk));
    const th = -Math.PI / 2 + (Math.PI * k) / steps;
    const ref = [Math.sin(th), vn[i][1] * Math.cos(th), vn[i][0] * Math.cos(th)];
    return dot(n, ref) < 0 ? scale(n, -1) : n;
  };
  for (let i = 0; i < N; i++) {
    const j = (i + 1) % N;
    const mid = [(ring[i][0] + ring[j][0]) / 2, (ring[i][1] + ring[j][1]) / 2];
    const mat = matOf(mid[0], mid[1], en[i]);
    if (mat == null) continue; // faixa que a câmera nunca vê (fundo do carro): não gera
    for (let k = 0; k < steps; k++) {
      const [a, b, c, d] = [G(i, k), G(j, k), G(j, k + 1), G(i, k + 1)];
      const [na, nb, nc, nd] = [normalAt(i, k, 1), normalAt(j, k, -1), normalAt(j, k + 1, -1), normalAt(i, k + 1, 1)];
      mesh.triN(mat, a, b, c, na, nb, nc);
      mesh.triN(mat, a, c, d, na, nc, nd);
    }
  }
  // Laterais: perfil recuado [r], em x = ±half(y, z) — malha densa, então a lateral estufa/recolhe junto com
  // half (barriga da porta, soleira recolhida, tumblehome); normal pela inclinação de half.
  const g = 1e-3;
  const capRing = ccw(inset);
  for (const s of [1, -1]) {
    const P = ([z, y]) => [s * half(y, z), y, z];
    const Nn = ([z, y]) => {
      const hy = (half(y + g, z) - half(y - g, z)) / (2 * g), hz = (half(y, z + g) - half(y, z - g)) / (2 * g);
      return norm([s, -s * hy, -s * hz]);
    };
    for (const [ia, ib, ic] of earclip(capRing)) {
      const [a, b, c] = [capRing[ia], capRing[ib], capRing[ic]];
      const n = capSubdiv;
      const Q = (i, j) => {
        const u = i / n, v = j / n, w = 1 - u - v;
        return [a[0] * w + b[0] * u + c[0] * v, a[1] * w + b[1] * u + c[1] * v];
      };
      const put = (q1, q2, q3) => mesh.triN(capMat, P(q1), P(q2), P(q3), Nn(q1), Nn(q2), Nn(q3));
      for (let i = 0; i < n; i++) {
        for (let j = 0; j < n - i; j++) {
          put(Q(i, j), Q(i + 1, j), Q(i, j + 1));
          if (i + j < n - 1) put(Q(i + 1, j), Q(i + 1, j + 1), Q(i, j + 1));
        }
      }
    }
  }
}

// ---------- peças ----------
const body = new Mesh();
// Lataria arredondada: ombros de raio BODY_R, teto/capô e para-choques abaulados no meio.
const BODY_R = 0.085, BODY_CROWN_TOP = 0.045, BODY_CROWN_END = 0.045, GLASS_R = 0.045;
/**
 * Material de cada faixa da lataria: caixa de roda = plástico preto; fundo do carro = não gera (a câmera não
 * desce abaixo de 2°, então o assoalho nunca aparece; economiza triângulos); o resto = pintura.
 */
const bodyStrip = (z, y, [nz, ny]) => {
  if ([FRONT_AXLE, REAR_AXLE].some((az) => Math.hypot(z - az, y - WHEEL_R) < ARCH_R + 0.02)) return M.trim;
  if (ny < -0.7 && y < 0.3) return null;
  return M.paint;
};
extrudeRound(body, bodyStrip, M.paint, bodyProfile, bodyHalf,
  { r: BODY_R, crownTop: BODY_CROWN_TOP, crownEnd: BODY_CROWN_END });
// Estufa: a faixa de cima entre para-brisa e vigia é o teto (pintado); o resto é vidro.
extrudeRound(body, (z, y, [nz, ny]) => (ny > 0.6 && z < 0.22 && z > -1.42 ? M.paint : M.glass), M.glass,
  glassProfile, glassHalf, { r: GLASS_R, crownTop: 0.04, crownEnd: 0.02 });

// Frente e traseira (perfil da foto) e "colar" peças nelas acompanhando o abaulado.
const frontZ = (y) => table([[0.364, 1.941], [0.549, 1.965], [0.697, 1.941], [0.789, 1.883], [0.845, 1.777]], y);
const rearZ = (y) => table([[0.475, -1.86], [0.66, -1.86], [0.789, -1.84], [0.919, -1.823], [1.011, -1.785],
  [1.067, -1.748], [1.14, -1.698], [1.27, -1.599], [1.455, -1.462]], y);
const onFrontAt = (off) => (x, y, o) => {
  const z = frontZ(y);
  return [x, y, z + bulge(x, bodyHalf(y, z), BODY_CROWN_END, BODY_R) + off + o * 0.01];
};
const onRearAt = (off) => (x, y, o) => {
  const z = rearZ(y);
  return [x, y, z - bulge(x, bodyHalf(y, z), BODY_CROWN_END, BODY_R) - off - o * 0.01];
};
const onFront = onFrontAt(0.004), onRearFace = onRearAt(0.004);
const FWD = () => [0, 0, 1], BACK = () => [0, 0, -1];
const circle = (cx, cy, r, n = 16) => Array.from({ length: n }, (_, i) => [cx + r * Math.cos((i / n) * 2 * Math.PI), cy + r * Math.sin((i / n) * 2 * Math.PI)]);
const rect = (x0, y0, x1, y1) => [[x0, y0], [x1, y0], [x1, y1], [x0, y1]];

for (const s of [1, -1]) {
  const onGlass = (extra) => (z, y, o) => [s * (glassHalf(y, z) + extra + o * 0.012), y, z];
  // Coluna A (cor do carro) acompanhando o para-brisa.
  body.slab(M.paint, [[0.905, 0.985], [0.32, 1.41], [0.25, 1.41], [0.85, 0.99]], onGlass(0.004), () => [s, 0, 0], true, 2);
  // Friso do teto (cor do carro) acima das janelas, até onde a lateral vira o ombro do teto.
  body.slab(M.paint, [[0.2, 1.41], [-0.26, 1.432], [-0.977, 1.435], [-1.192, 1.42], [-1.38, 1.418],
    [-1.157, 1.448], [-0.888, 1.478], [-0.529, 1.483], [0.009, 1.448], [0.2, 1.418]], onGlass(0.004), () => [s, 0, 0], true, 2);
  // Coluna C larga, pintada, entre o vidro fixo da porta traseira e a traseira.
  body.slab(M.paint, chaikin([[-1.192, 1.415], [-1.4, 1.415], [-1.555, 1.27], [-1.655, 1.14],
    [-1.7, 1.06], [-1.264, 1.06], [-1.24, 1.3]], 1), onGlass(0.004), () => [s, 0, 0], true, 2);
}
// Frente: grade preta com dois frisos cromados na altura dos faróis, emblema (disco genérico), colmeia
// embaixo, neblina redonda em moldura preta (apagada: não há sinal da neblina) e placa em branco.
body.slab(M.trim, [[-0.29, 0.675], [0.29, 0.675], [0.32, 0.772], [-0.32, 0.772]], onFront, FWD, false, 4);
for (const y of [0.688, 0.722, 0.758]) body.slab(M.trim, rect(-0.3, y - 0.006, 0.3, y + 0.006), onFrontAt(0.012), FWD, false, 3);
for (const y of [0.745, 0.7]) body.slab(M.chrome, rect(-0.31, y - 0.004, 0.31, y + 0.004), onFrontAt(0.016), FWD, false, 3);
body.slab(M.chrome, circle(0, 0.722, 0.055, 20), onFrontAt(0.018), FWD, false, 2);
// Grade de baixo: fundo grafite com a colmeia preta em relevo por cima (hexágonos em fileiras alternadas).
body.slab(M.interior, [[-0.5, 0.35], [0.5, 0.35], [0.48, 0.49], [-0.48, 0.49]], onFront, FWD, false, 5);
for (let row = 0; row < 5; row++) {
  const y = 0.367 + row * 0.026;
  for (let x = -0.47 + (row % 2) * 0.016; x <= 0.47; x += 0.032) {
    if (Math.abs(x) < 0.22 && y > 0.47) continue; // atrás da placa
    body.decal(M.trim, circle(x, y, 0.0125, 6), onFrontAt(0.012), FWD);
  }
}
for (const s of [1, -1]) {
  body.slab(M.trim, circle(s * 0.56, 0.4, 0.07), onFrontAt(0.006), FWD, false, 2);
  body.slab(M.lamp_fog, circle(s * 0.56, 0.4, 0.04), onFrontAt(0.018), FWD, false, 2);
}
body.slab(M.plate, rect(-0.2, 0.485, 0.2, 0.615), onFrontAt(0.02), FWD, false, 3);
// Traseira: placa em branco, engate com bola (o Fox do dono tem), sensores de ré e refletores no para-choque.
body.slab(M.plate, rect(-0.2, 0.53, 0.2, 0.655), onRearAt(0.006), BACK, false, 3);
body.box(M.trim, [0, 0.3, -1.9], [0.08, 0.06, 0.14]);
body.box(M.chrome, [0, 0.36, -1.95], [0.05, 0.05, 0.05]);
for (const s of [1, -1]) {
  for (const x of [0.36, 0.56]) body.decal(M.trim, circle(s * x, 0.5, 0.013, 10), onRearAt(0.002), BACK);
  body.slab(M.lamp_tail, rect(Math.min(s * 0.54, s * 0.66), 0.35, Math.max(s * 0.54, s * 0.66), 0.372), onRearAt(0.002), BACK, false, 2);
}
{
  const P0 = [0.905, 0.985], dir = norm([0.26 - 0.905, 1.473 - 0.985]), n2 = [-dir[1], dir[0]];
  const wsCrown = 0.04 * n2[1] ** 2 + 0.02 * n2[0] ** 2;
  const onShield = (x, sAlong, o) => {
    const z = P0[0] + dir[0] * sAlong, y = P0[1] + dir[1] * sAlong;
    const d = Math.max(0, bulge(x, glassHalf(y, z), wsCrown, GLASS_R)) + 0.012 + o * 0.012;
    return [x, y + n2[1] * d, z + n2[0] * d];
  };
  body.slab(M.trim, [[-0.45, 0.07], [0.06, 0.09], [0.06, 0.102], [-0.45, 0.082]], onShield, () => [0, n2[1], n2[0]], false, 4);
  body.slab(M.trim, [[0.1, 0.075], [0.55, 0.095], [0.55, 0.107], [0.1, 0.087]], onShield, () => [0, n2[1], n2[0]], false, 4);
}
// Teto: antena "barbatana" preta (na foto, sobre a porta traseira), sobre o teto abaulado.
body.slab(M.trim, [[-0.84, 1.566], [-1.0, 1.566], [-0.96, 1.64], [-0.92, 1.645]],
  (z, y, o) => [(o - 0.5) * 0.05, y, z], () => [1, 0, 0]);
// Portinhola de combustível: lado direito (−X), entre a caixa de roda e a lanterna (foto lateral).
// O vão em volta da tampa aparece como um contorno escuro fino.
body.slab(M.trim, chaikin([[-1.434, 0.814], [-1.606, 0.814], [-1.606, 0.966], [-1.434, 0.966]], 2),
  (z, y, o) => [-(bodyHalf(y, z) + 0.008 + o * 0.001), y, z], () => [-1, 0, 0], true, 2);
body.slab(M.paint, chaikin([[-1.44, 0.82], [-1.6, 0.82], [-1.6, 0.96], [-1.44, 0.96]], 2),
  (z, y, o) => [-(bodyHalf(y, z) + 0.01 + o * 0.004), y, z], () => [-1, 0, 0], true, 2);

/**
 * Porta do lado esquerdo (+X), coordenadas do mundo, desenhada pela foto: [panel] = chapa (borda de cima logo
 * abaixo do ombro da cintura), [glass] = janela, [black] = plásticos pretos por cima do vidro.
 */
function door(opts) {
  const m = new Mesh();
  // Camadas na lateral: carroceria (0) < vão escuro (+9 mm) < forro da porta (+13 mm) < porta (+15 mm).
  const outer = (half, extra = 0) => (z, y, o) => [half(y, z) + 0.015 + extra + o * 0.022, y, z];
  // Borda preta: vira a fresta entre a porta e a carroceria (linha de vão contínua, sem serrilhar).
  m.slab(M.paint, opts.panel, outer(bodyHalf), () => [1, 0, 0], true, 5, M.trim);
  m.decal(M.interior, opts.panel, (z, y, o) => [bodyHalf(y, z) + 0.0135, y, z], () => [-1, 0, 0], 2); // forro: só a face de dentro
  m.slab(M.glass, opts.glass, outer(glassHalf), () => [1, 0, 0], true, 3);
  for (const poly of opts.black) m.slab(M.trim, poly, outer(glassHalf, 0.003), () => [1, 0, 0], true, 2);
  // Friso lateral fino na cor do carro e maçaneta arredondada na cor do carro.
  const [z0, z1] = opts.molding;
  // Friso fino (4 mm de relevo): mais grosso, a borda vira uma linha clara serrilhada vista de lado.
  m.slab(M.paint, [[z0, 0.44], [z1, 0.44], [z1, 0.46], [z0, 0.46]], (z, y, o) => [bodyHalf(y, z) + 0.037 + o * 0.004, y, z], () => [1, 0, 0], true, 2);
  const [hz, hy] = opts.handle;
  m.blob(M.paint, [bodyHalf(hy, hz) + 0.034, hy, hz], [0.014, 0.016, 0.065], 2.4, 12, 6);
  if (opts.mirror) {
    // Retrovisor: base preta e concha arredondada na cor do carro (como na foto).
    const x = bodyHalf(1, 0.64);
    m.blob(M.trim, [x + 0.03, 0.975, 0.69], [0.035, 0.03, 0.05], 2.4, 12, 8);
    m.blob(M.paint, [x + 0.1, 0.995, 0.64], [0.07, 0.06, 0.095], 2.6, 20, 12);
  }
  m.panel = opts.panel;
  return m;
}
const FRONT_HINGE_Z = 0.771, REAR_HINGE_Z = -0.314;
/** Topo das portas: logo abaixo do ombro da cintura (a faixa do ombro fica na carroceria, como um friso). */
const doorTop = (z) => beltY(z) - 0.06;
const doorFront = door({
  panel: [[0.744, 0.33], [FRONT_HINGE_Z, doorTop(FRONT_HINGE_Z)], [0.546, doorTop(0.546)], [-0.287, doorTop(-0.287)], [-0.287, 0.33]],
  glass: [[0.88, 1.008], [0.224, 1.415], [-0.26, 1.43], [-0.26, 0.998], [0.546, 0.95]],
  black: [
    [[0.905, 1.01], [0.72, 0.985], [0.737, 1.1]], // triângulo do retrovisor
    [[-0.2, 0.997], [-0.26, 0.998], [-0.26, 1.43], [-0.2, 1.428]], // coluna B
  ],
  molding: [0.7, -0.25], handle: [-0.171, 0.817], mirror: true,
});
const doorRear = door({
  panel: [[REAR_HINGE_Z, 0.33], [REAR_HINGE_Z, doorTop(REAR_HINGE_Z)], [-1.246, 1.0], [-1.264, 1.0],
    [-1.32, 0.919], [-1.291, 0.771], [-1.21, 0.622], [-1.067, 0.492], [-0.941, 0.381], [-0.9, 0.33]],
  glass: [[REAR_HINGE_Z, beltY(REAR_HINGE_Z) + 0.01], [REAR_HINGE_Z, 1.435], [-0.977, 1.435], [-1.192, 1.415],
    [-1.24, 1.3], [-1.264, 1.075], [-0.42, 1.02]],
  black: [
    [[REAR_HINGE_Z, 1.015], [-0.36, 1.018], [-0.36, 1.435], [REAR_HINGE_Z, 1.435]], // coluna B
    [[-0.995, 1.05], [-1.022, 1.055], [-1.022, 1.435], [-0.995, 1.435]], // divisão do vidro fixo
  ],
  molding: [-0.33, -0.94], handle: [-1.148, 0.86],
});
for (const s of [1, -1]) {
  for (const poly of [doorFront.panel, doorRear.panel]) {
    body.decal(M.interior, poly, (z, y, o) => [s * (bodyHalf(y, z) + 0.0105), y, z], () => [s, 0, 0], 2);
  }
}
/** Repetidor de seta embaixo da concha do retrovisor (coordenadas do mundo, lado esquerdo). */
const mirrorRepeater = [bodyHalf(1, 0.64) + 0.13, 0.938, 0.6];

// Capô: da base do para-brisa até a grade, na altura do perfil, com o abaulado da lataria.
const hoodY = (z) => table([[0.905, 0.99], [1.013, 0.985], [1.294, 0.958], [1.53, 0.919], [1.777, 0.845], [1.87, 0.8]], z);
const hood = new Mesh();
// Dois vincos longitudinais (Fox G2) que se aproximam do meio em direção à grade: viram linhas de reflexo.
const hoodCrease = (x, v) => {
  const cx = lerp(0.36, 0.29, v);
  return 0.007 * Math.exp(-(((Math.abs(x) - cx) / 0.028) ** 2));
};
hood.gridPatch(M.paint, 64, 34, (u, v, o) => {
  const z = lerp(0.95, 1.86, v), xmax = lerp(0.7, 0.62, v), x = (u * 2 - 1) * xmax, y0 = hoodY(z);
  const y = y0 + Math.max(0, bulge(x, bodyHalf(y0, z), BODY_CROWN_TOP * 0.95, BODY_R)) + hoodCrease(x, v);
  return [x, y + 0.008 + o * 0.016, z];
}, [0, 1, 0], M.trim, [8, 4]); // por baixo quase nunca aparece (o capô não abre: sem sinal no CAN)

// Tampa traseira: chapa e vigia seguindo o caimento da traseira (perfil da foto) e o abaulado.
const tailgate = new Mesh();
const rearBulge = (x, y) => Math.max(0, bulge(x, (y < 1.06 ? bodyHalf : glassHalf)(y, rearZ(y)), y < 1.06 ? BODY_CROWN_END : 0.03, BODY_R));
const onRear = (x, y, o) => [x, y, rearZ(y) - rearBulge(x, y) - 0.01 - o * 0.02];
tailgate.slab(M.paint, [[-0.55, 0.66], [0.55, 0.66], [0.58, 1.06], [-0.58, 1.06]], onRear, BACK, false, 8, M.trim);
tailgate.slab(M.glass, [[-0.58, 1.06], [0.58, 1.06], [0.47, 1.395], [-0.47, 1.395]], onRear, BACK, false, 8);
// Limpador traseiro: eixo + palheta deitada no vidro.
tailgate.box(M.trim, [0, 1.12, rearZ(1.12) - rearBulge(0, 1.12) - 0.04], [0.04, 0.03, 0.04]);
tailgate.box(M.trim, [0.17, 1.15, rearZ(1.15) - rearBulge(0.17, 1.15) - 0.03], [0.36, 0.012, 0.012]);
// Emblema (disco genérico) e a câmera de ré logo abaixo (confirmada pelo dono).
tailgate.slab(M.chrome, circle(0, 0.93, 0.055, 20), (x, y, o) => onRear(x, y, 1 + o * 0.5), BACK, false, 2);
tailgate.slab(M.trim, circle(0, 0.845, 0.014, 10), (x, y, o) => onRear(x, y, 1 + o * 0.5), BACK);
// Aerofólio no fim do teto (preso na tampa, sobe junto) com a luz de freio alta, apagada: não há sinal do
// pedal de freio.
tailgate.slab(M.paint, chaikin([[-1.36, 1.53], [-1.42, 1.527], [-1.5, 1.485], [-1.515, 1.46], [-1.46, 1.47], [-1.38, 1.5]], 1),
  (z, y, o) => [(o - 0.5) * 1.14, y, z], () => [1, 0, 0]);
tailgate.box(M.lamp_tail, [0, 1.47, -1.49], [0.24, 0.02, 0.03]);

/** Roda de liga 15" do Fox do dono: 5 pares de raios em V, aro prata, miolo escuro, calota central. */
const wheel = (left) => {
  const m = new Mesh();
  const s = left ? 1 : -1;
  const face = s * WHEEL_W / 2;
  const w = WHEEL_W / 2, R = WHEEL_R;
  // Perfil do pneu (x, raio): flanco, ombro arredondado, banda, ombro, flanco.
  m.revolveX(M.tire, [[w - 0.004, RIM_R + 0.004], [w, RIM_R + 0.03], [w + 0.004, R - 0.06], [w, R - 0.03],
    [w - 0.015, R - 0.008], [w - 0.035, R], [-(w - 0.035), R], [-(w - 0.015), R - 0.008], [-w, R - 0.03],
    [-(w + 0.004), R - 0.06], [-w, RIM_R + 0.03], [-(w - 0.004), RIM_R + 0.004]], 48);
  m.ringX(M.tire, 0, RIM_R + 0.03, -face, -s, 24); // lado de dentro fechado
  m.ringX(M.trim, 0, RIM_R, face - s * 0.05, s, 24); // miolo escuro (aparece entre os raios)
  m.cylinderX(M.rim, RIM_R, 0.05, 48, s > 0 ? face - 0.05 : face, false); // tala
  m.ringX(M.rim, RIM_R - 0.02, RIM_R + 0.004, face + s * 0.004, s, 48); // borda do aro
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
  const h = hinge(bodyHalf(0.6, z) + 0.026, z);
  const local = mesh.relativeTo(h);
  // Repetidor de seta do retrovisor: nó próprio (o app pisca pelo nome), filho da porta (abre junto).
  const kids = [];
  if (mesh === doorFront) {
    const rel = sub(mirrorRepeater, h);
    kids.push(node(left ? "lamp_turn_ml" : "lamp_turn_mr", boxMesh(M.lamp_turn, [0.05, 0.012, 0.045]), left ? rel : [-rel[0], rel[1], rel[2]]));
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
// As lentes são chapas finas coladas na lataria (acompanham o abaulado e a quina), não caixas.
const lens = (mat, poly, map, out, n = 4) => { const m = new Mesh(); m.slab(mat, poly, map, out, false, n); return m; };
for (const [side, s] of [["l", 1], ["r", -1]]) {
  const pick = (m) => (s > 0 ? m : m.mirrorX());
  const lampNode = (name, m) => children.push(node(`${name}_${side}`, pick(m)));
  // Farol com máscara escura, mais largo por fora e subindo para a quina; a seta âmbar numa faixa embaixo.
  const head = lens(M.lamp_head, [[0.31, 0.675], [0.62, 0.668], [0.655, 0.755], [0.6, 0.8], [0.31, 0.785]], onFront, () => [0, 0, 1]);
  for (const [cx, r] of [[0.4, 0.045], [0.53, 0.05]]) {
    head.slab(M.chrome, circle(cx, 0.735, r, 18), onFrontAt(0.015), FWD, false, 2);
    head.slab(M.lamp_head, circle(cx, 0.735, r * 0.45, 12), onFrontAt(0.026), FWD);
  }
  lampNode("lamp_head", head);
  children.push(node(`lamp_turn_f${side}`, pick(lens(M.lamp_turn, [[0.4, 0.648], [0.61, 0.643], [0.625, 0.664], [0.4, 0.668]], onFront, () => [0, 0, 1]))));
  // Lanterna G2: vermelho por fora, dobrando a quina para a lateral; a seta na parte de dentro, mais baixa.
  const tail = lens(M.lamp_tail, [[0.55, 0.8], [0.69, 0.8], [0.69, 1.04], [0.59, 1.04], [0.55, 0.94]], onRearFace, () => [0, 0, -1]);
  tail.slab(M.lamp_tail, [[-1.73, 0.8], [-1.64, 0.84], [-1.64, 0.98], [-1.73, 1.04]],
    (z, y, o) => [bodyHalf(y, z) + 0.004 + o * 0.01, y, z], () => [1, 0, 0], false, 3);
  lampNode("lamp_tail", tail);
  children.push(node(`lamp_turn_r${side}`, pick(lens(M.lamp_turn, [[0.46, 0.8], [0.545, 0.8], [0.545, 0.92], [0.46, 0.92]], onRearFace, () => [0, 0, -1]))));
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
