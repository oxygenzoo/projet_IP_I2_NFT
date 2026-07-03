import { Component, computed, input } from '@angular/core';

type QrBlock = { dataCodewords: number; eccCodewords: number };
type QrPlan = { version: number; blocks: QrBlock[] };
type QrCell = { x: number; y: number };

const PLANS: QrPlan[] = [
  { version: 1, blocks: [{ dataCodewords: 19, eccCodewords: 7 }] },
  { version: 2, blocks: [{ dataCodewords: 34, eccCodewords: 10 }] },
  { version: 3, blocks: [{ dataCodewords: 55, eccCodewords: 15 }] },
  { version: 4, blocks: [{ dataCodewords: 80, eccCodewords: 20 }] },
  { version: 5, blocks: [{ dataCodewords: 108, eccCodewords: 26 }] },
  { version: 6, blocks: [{ dataCodewords: 68, eccCodewords: 18 }, { dataCodewords: 68, eccCodewords: 18 }] },
  { version: 7, blocks: [{ dataCodewords: 78, eccCodewords: 20 }, { dataCodewords: 78, eccCodewords: 20 }] },
  { version: 8, blocks: [{ dataCodewords: 97, eccCodewords: 24 }, { dataCodewords: 97, eccCodewords: 24 }] },
  { version: 9, blocks: [{ dataCodewords: 116, eccCodewords: 30 }, { dataCodewords: 116, eccCodewords: 30 }] },
  {
    version: 10,
    blocks: [
      { dataCodewords: 68, eccCodewords: 18 },
      { dataCodewords: 68, eccCodewords: 18 },
      { dataCodewords: 69, eccCodewords: 18 },
      { dataCodewords: 69, eccCodewords: 18 },
    ],
  },
];

const ALIGNMENT_POSITIONS: Record<number, number[]> = {
  1: [],
  2: [6, 18],
  3: [6, 22],
  4: [6, 26],
  5: [6, 30],
  6: [6, 34],
  7: [6, 22, 38],
  8: [6, 24, 42],
  9: [6, 26, 46],
  10: [6, 28, 50],
};

@Component({
  selector: 'app-qr-code',
  templateUrl: './qr-code.component.html',
  styleUrl: './qr-code.component.scss',
})
export class QrCodeComponent {
  readonly value = input.required<string>();
  readonly label = input('QR code');

  protected readonly quietZone = 4;
  protected readonly matrix = computed(() => makeQr(this.value()));
  protected readonly cells = computed(() => matrixCells(this.matrix()));
  protected readonly backgroundOrigin = computed(() => -this.quietZone);
  protected readonly backgroundSize = computed(() => this.matrix().length + this.quietZone * 2);
  protected readonly viewBox = computed(() => `${-this.quietZone} ${-this.quietZone} ${this.backgroundSize()} ${this.backgroundSize()}`);
}

function matrixCells(matrix: boolean[][]): QrCell[] {
  const cells: QrCell[] = [];
  matrix.forEach((row, y) => {
    row.forEach((dark, x) => {
      if (dark) {
        cells.push({ x, y });
      }
    });
  });
  return cells;
}

function makeQr(value: string): boolean[][] {
  const bytes = Array.from(new TextEncoder().encode(value));
  const plan = selectPlan(bytes.length);
  const size = plan.version * 4 + 17;
  const modules = makeMatrix<boolean | null>(size, null);
  const reserved = makeMatrix(size, false);

  drawPatterns(modules, reserved, plan.version);
  const data = encodeData(bytes, plan);
  const codewords = addErrorCorrection(data, plan);
  placeData(modules, reserved, codewords);

  let bestMatrix = modules as boolean[][];
  let bestPenalty = Number.POSITIVE_INFINITY;
  for (let mask = 0; mask < 8; mask++) {
    const candidate = cloneMatrix(modules);
    applyMask(candidate, reserved, mask);
    drawFormat(candidate, reserved, mask);
    const penalty = score(candidate as boolean[][]);
    if (penalty < bestPenalty) {
      bestPenalty = penalty;
      bestMatrix = candidate as boolean[][];
    }
  }

  return bestMatrix;
}

function selectPlan(byteLength: number): QrPlan {
  const plan = PLANS.find((candidate) => {
    const dataCodewords = candidate.blocks.reduce((sum, block) => sum + block.dataCodewords, 0);
    const charCountBits = candidate.version < 10 ? 8 : 16;
    return 4 + charCountBits + byteLength * 8 <= dataCodewords * 8;
  });

  if (!plan) {
    throw new Error('Contribution link is too long for the local QR generator.');
  }

  return plan;
}

function encodeData(bytes: number[], plan: QrPlan): number[] {
  const bitLength = plan.blocks.reduce((sum, block) => sum + block.dataCodewords, 0) * 8;
  const bits: number[] = [];
  appendBits(bits, 0b0100, 4);
  appendBits(bits, bytes.length, plan.version < 10 ? 8 : 16);
  bytes.forEach((byte) => appendBits(bits, byte, 8));

  const terminator = Math.min(4, bitLength - bits.length);
  appendBits(bits, 0, terminator);
  while (bits.length % 8 !== 0) {
    bits.push(0);
  }

  const data: number[] = [];
  for (let index = 0; index < bits.length; index += 8) {
    data.push(bits.slice(index, index + 8).reduce((byte, bit) => (byte << 1) | bit, 0));
  }

  for (let pad = 0; data.length < bitLength / 8; pad++) {
    data.push(pad % 2 === 0 ? 0xec : 0x11);
  }

  return data;
}

function appendBits(bits: number[], value: number, length: number): void {
  for (let shift = length - 1; shift >= 0; shift--) {
    bits.push((value >>> shift) & 1);
  }
}

function addErrorCorrection(data: number[], plan: QrPlan): number[] {
  const blocks: { data: number[]; ecc: number[] }[] = [];
  let offset = 0;

  for (const block of plan.blocks) {
    const dataBlock = data.slice(offset, offset + block.dataCodewords);
    offset += block.dataCodewords;
    blocks.push({ data: dataBlock, ecc: reedSolomon(dataBlock, block.eccCodewords) });
  }

  const result: number[] = [];
  const maxData = Math.max(...blocks.map((block) => block.data.length));
  for (let index = 0; index < maxData; index++) {
    blocks.forEach((block) => {
      if (index < block.data.length) {
        result.push(block.data[index]);
      }
    });
  }

  const maxEcc = Math.max(...blocks.map((block) => block.ecc.length));
  for (let index = 0; index < maxEcc; index++) {
    blocks.forEach((block) => result.push(block.ecc[index]));
  }

  return result;
}

function reedSolomon(data: number[], degree: number): number[] {
  const generator = rsGenerator(degree);
  const result = [...data, ...Array(degree).fill(0)];

  for (let index = 0; index < data.length; index++) {
    const coefficient = result[index];
    if (coefficient !== 0) {
      generator.forEach((factor, generatorIndex) => {
        result[index + generatorIndex] ^= gfMultiply(factor, coefficient);
      });
    }
  }

  return result.slice(data.length);
}

function rsGenerator(degree: number): number[] {
  let result = [1];
  for (let index = 0; index < degree; index++) {
    const next = Array(result.length + 1).fill(0);
    result.forEach((coefficient, coefficientIndex) => {
      next[coefficientIndex] ^= gfMultiply(coefficient, 1);
      next[coefficientIndex + 1] ^= gfMultiply(coefficient, gfPow(index));
    });
    result = next;
  }
  return result;
}

function gfPow(power: number): number {
  let value = 1;
  for (let index = 0; index < power; index++) {
    value = gfMultiply(value, 2);
  }
  return value;
}

function gfMultiply(left: number, right: number): number {
  let result = 0;
  for (let index = 0; index < 8; index++) {
    if ((right & 1) !== 0) {
      result ^= left;
    }
    const carry = (left & 0x80) !== 0;
    left = (left << 1) & 0xff;
    if (carry) {
      left ^= 0x1d;
    }
    right >>>= 1;
  }
  return result;
}

function drawPatterns(modules: (boolean | null)[][], reserved: boolean[][], version: number): void {
  const size = modules.length;
  drawFinder(modules, reserved, 0, 0);
  drawFinder(modules, reserved, size - 7, 0);
  drawFinder(modules, reserved, 0, size - 7);

  for (let index = 8; index < size - 8; index++) {
    setModule(modules, reserved, index, 6, index % 2 === 0);
    setModule(modules, reserved, 6, index, index % 2 === 0);
  }

  for (const row of ALIGNMENT_POSITIONS[version]) {
    for (const col of ALIGNMENT_POSITIONS[version]) {
      if (reserved[row]?.[col]) {
        continue;
      }
      drawAlignment(modules, reserved, col - 2, row - 2);
    }
  }

  setModule(modules, reserved, 8, size - 8, true);
  reserveFormatAreas(reserved);
}

function drawFinder(modules: (boolean | null)[][], reserved: boolean[][], x: number, y: number): void {
  for (let dy = -1; dy <= 7; dy++) {
    for (let dx = -1; dx <= 7; dx++) {
      const xx = x + dx;
      const yy = y + dy;
      if (!inBounds(modules.length, xx, yy)) {
        continue;
      }
      const dark = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6 && (dx === 0 || dx === 6 || dy === 0 || dy === 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4));
      setModule(modules, reserved, xx, yy, dark);
    }
  }
}

function drawAlignment(modules: (boolean | null)[][], reserved: boolean[][], x: number, y: number): void {
  for (let dy = 0; dy < 5; dy++) {
    for (let dx = 0; dx < 5; dx++) {
      const dark = dx === 0 || dx === 4 || dy === 0 || dy === 4 || (dx === 2 && dy === 2);
      setModule(modules, reserved, x + dx, y + dy, dark);
    }
  }
}

function reserveFormatAreas(reserved: boolean[][]): void {
  const size = reserved.length;
  for (let index = 0; index < 9; index++) {
    reserved[8][index] = true;
    reserved[index][8] = true;
    reserved[8][size - 1 - index] = true;
    reserved[size - 1 - index][8] = true;
  }
}

function placeData(modules: (boolean | null)[][], reserved: boolean[][], codewords: number[]): void {
  const bits = codewords.flatMap((byte) => Array.from({ length: 8 }, (_, index) => (byte >>> (7 - index)) & 1));
  const size = modules.length;
  let bitIndex = 0;
  let upward = true;

  for (let right = size - 1; right >= 1; right -= 2) {
    if (right === 6) {
      right--;
    }
    for (let vertical = 0; vertical < size; vertical++) {
      const y = upward ? size - 1 - vertical : vertical;
      for (let col = 0; col < 2; col++) {
        const x = right - col;
        if (!reserved[y][x]) {
          modules[y][x] = bitIndex < bits.length ? bits[bitIndex] === 1 : false;
          bitIndex++;
        }
      }
    }
    upward = !upward;
  }
}

function applyMask(modules: (boolean | null)[][], reserved: boolean[][], mask: number): void {
  modules.forEach((row, y) => {
    row.forEach((value, x) => {
      if (!reserved[y][x] && maskValue(mask, x, y)) {
        row[x] = !value;
      }
    });
  });
}

function maskValue(mask: number, x: number, y: number): boolean {
  switch (mask) {
    case 0: return (x + y) % 2 === 0;
    case 1: return y % 2 === 0;
    case 2: return x % 3 === 0;
    case 3: return (x + y) % 3 === 0;
    case 4: return (Math.floor(y / 2) + Math.floor(x / 3)) % 2 === 0;
    case 5: return ((x * y) % 2) + ((x * y) % 3) === 0;
    case 6: return (((x * y) % 2) + ((x * y) % 3)) % 2 === 0;
    default: return (((x + y) % 2) + ((x * y) % 3)) % 2 === 0;
  }
}

function drawFormat(modules: (boolean | null)[][], reserved: boolean[][], mask: number): void {
  const size = modules.length;
  const bits = formatBits(mask);
  const positionsA = [[8, 0], [8, 1], [8, 2], [8, 3], [8, 4], [8, 5], [8, 7], [8, 8], [7, 8], [5, 8], [4, 8], [3, 8], [2, 8], [1, 8], [0, 8]];
  const positionsB = [[size - 1, 8], [size - 2, 8], [size - 3, 8], [size - 4, 8], [size - 5, 8], [size - 6, 8], [size - 7, 8], [size - 8, 8], [8, size - 7], [8, size - 6], [8, size - 5], [8, size - 4], [8, size - 3], [8, size - 2], [8, size - 1]];
  positionsA.forEach(([x, y], index) => setModule(modules, reserved, x, y, ((bits >>> index) & 1) !== 0));
  positionsB.forEach(([x, y], index) => setModule(modules, reserved, x, y, ((bits >>> index) & 1) !== 0));
}

function formatBits(mask: number): number {
  let data = (0b01 << 3) | mask;
  let bits = data << 10;
  const generator = 0b10100110111;
  for (let index = 14; index >= 10; index--) {
    if (((bits >>> index) & 1) !== 0) {
      bits ^= generator << (index - 10);
    }
  }
  return (((data << 10) | bits) ^ 0b101010000010010) & 0x7fff;
}

function score(matrix: boolean[][]): number {
  const size = matrix.length;
  let penalty = 0;

  for (let y = 0; y < size; y++) {
    penalty += linePenalty(matrix[y]);
  }
  for (let x = 0; x < size; x++) {
    penalty += linePenalty(matrix.map((row) => row[x]));
  }

  for (let y = 0; y < size - 1; y++) {
    for (let x = 0; x < size - 1; x++) {
      const value = matrix[y][x];
      if (matrix[y][x + 1] === value && matrix[y + 1][x] === value && matrix[y + 1][x + 1] === value) {
        penalty += 3;
      }
    }
  }

  const dark = matrix.flat().filter(Boolean).length;
  penalty += Math.floor(Math.abs((dark * 100) / (size * size) - 50) / 5) * 10;
  return penalty;
}

function linePenalty(line: boolean[]): number {
  let penalty = 0;
  let runColor = line[0];
  let runLength = 1;
  for (let index = 1; index < line.length; index++) {
    if (line[index] === runColor) {
      runLength++;
    } else {
      if (runLength >= 5) {
        penalty += 3 + runLength - 5;
      }
      runColor = line[index];
      runLength = 1;
    }
  }
  return runLength >= 5 ? penalty + 3 + runLength - 5 : penalty;
}

function setModule(modules: (boolean | null)[][], reserved: boolean[][], x: number, y: number, value: boolean): void {
  modules[y][x] = value;
  reserved[y][x] = true;
}

function makeMatrix<T>(size: number, value: T): T[][] {
  return Array.from({ length: size }, () => Array(size).fill(value));
}

function cloneMatrix<T>(matrix: T[][]): T[][] {
  return matrix.map((row) => [...row]);
}

function inBounds(size: number, x: number, y: number): boolean {
  return x >= 0 && y >= 0 && x < size && y < size;
}
