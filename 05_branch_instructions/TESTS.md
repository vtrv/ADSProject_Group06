# Branch/Jump Test Plan

## Gaps in the existing `RV32I_BranchJumpTester`

- Each of the 6 branch types is tested **once, in one polarity only** — a comparator bug that makes BLT always-taken would pass.
- **No operand-order symmetry**: `(a, b)` is never also checked as `(b, a)`, so swapped-operand bugs in BGE/BGEU are invisible.
- **No signed/unsigned discrimination** on the same pair (e.g. `(-1, 5)` where BLT and BLTU disagree).
- **No backward branches** — `immB` sign extension and loops are untested.
- **Immediate bit-scattering barely exercised**: all offsets are ≤ 12, so scattered bits (`imm[11]` at `instr(7)` for B-type, upper fields of `immJ`) are never set.
- **Flush correctness is asserted weakly**: expecting `result == 0` in flushed slots does not verify the wrong-path destination register was left unchanged. A flush that zeroes `uop` but leaves `wrEn` set would write 0 and still pass.
- **JALR details untested**: LSB clearing (`& ~1`), negative base offset, and the call/return pattern.

---

## Infrastructure: in-testbench helpers

Add a small helper object (`AsmProgram.scala`) with:

- Encoder functions per instruction: `ADDI(rd, rs1, imm)`, `BEQ(rs1, rs2, off)`, `JAL(rd, off)`, `JALR(rd, rs1, imm)`, …
- `runProgram(name, instrs)(body: dut => Unit)` — writes a hex file to `target/test-programs/<name>`, instantiates `PipelinedRV32I` on it, and steps past the 5-cycle pipeline fill.
- Helpers: `expectResult(v, label)` (step + expect), `expectBubbles(n)` (n flushed-NOP slots), `skipSlot()` (step without asserting — for branch/jump slots whose ALU output is an implementation detail).

Each scenario becomes ~10 lines with the program and expected WB stream side by side.

---

## A. Comparator semantics — symmetric pair matrix

For each branch type, run a take-or-skip gadget (`Bxx rs1, rs2, +8` over a poison instruction) **in both operand orders**. The pairs cover all boundary conditions:

| Pair | BEQ | BNE | BLT | BGE | BLTU | BGEU |
|---|---|---|---|---|---|---|
| `(5, 5)` equal | T | N | N | T | N | T |
| `(3, 7)` | N | T | T | N | T | N |
| `(7, 3)` swapped | N | T | N | T | N | T |
| `(-1, 5)` signed/unsigned split | N | T | **T** | N | **N** | **T** |
| `(5, -1)` swapped | N | T | **N** | **T** | **T** | **N** |
| `(INT_MIN, INT_MAX)` boundary | N | T | T | N | N | T |
| `(INT_MAX, INT_MIN)` swapped | N | T | N | T | T | N |
| `(-7, -3)` both negative | N | T | T | N | T | N |
| same register `Bxx x1, x1` | T | N | N | T | N | T |

Cells marked **bold** are where signed and unsigned comparison disagree — the highest-value checks.

---

## B. Target address & immediate encoding

1. **Backward branch (loop)**: `x2=0; x1=3; loop: x2+=1; x1-=1; BNE x1, x0, -8` → verify `x2 == 3`. Tests negative `immB`, repeated taken → final not-taken, and distance-1 forwarding of the loop counter into the branch decision.
2. **immB bit-field coverage**: one forward branch with a large offset (e.g. `+0xA14`) that sets bits in every scattered field (`imm[11]` at `instr(7)`, `imm[10:5]`, `imm[4:1]`), landing on a known marker. A miswired bit in IDstage.scala:86 would land at the wrong address.
3. **immJ bit-field coverage**: JAL with an offset that exercises `imm[19:12]` and `imm[11]` (e.g. `+0x1804`), plus one backward JAL.
4. **Branch to PC+4**: `BEQ x1, x2, +4` taken — redirect target equals the sequential PC. Should still cost 2 bubbles.

---

## C. Flush / control-hazard correctness

1. **Wrong-path writes must not retire**: seed x8 beforehand; take a branch over `ADDI x8, x0, 111`; then read x8 and expect its seeded value. Catches "uop cleared but `wrEn` left set".
2. **Wrong-path branch must not redirect**: place a would-be-taken branch/JAL in the two flushed slots after a taken branch. Execution must continue at the first target, not the nested redirect.
3. **Wrong-path invalid instruction must not trap**: place `0xFFFFFFFF` (garbage encoding) in a flushed slot; `exception` must stay false throughout.
4. **Exact flush penalty — taken**: taken branch/jump produces exactly 2 bubbles, then the target instruction. Use `expectBubbles(2)`.
5. **Exact fall-through — not-taken**: not-taken branch produces 0 bubbles; the next sequential instruction retires immediately.
6. **Branch at branch target**: taken branch whose target is itself a taken branch — two consecutive redirects.
7. **B-type must not write a register**: seed x12; take `BEQ x1, x2, +12` (whose `rd` field bits alias to x12 in the encoding); verify x12 is unchanged after. Guards `wrEn` decoding for B-type in IDstage.scala:258.

---

## D. Forwarding into branch operands (symmetric by operand slot)

Stale comparands can silently flip control flow, making forwarding-into-branch the highest-risk interaction:

- **Distance-1 from MEM → rs1**: produce a value in the immediately preceding instruction; branch uses it as left operand. Repeat with that value in **rs2** (same distance, opposite slot).
- **Distance-2 from WB → rs1**, mirrored for **rs2**.
- **Both operands forwarded from different stages into one branch**: distance-1 into rs1, distance-2 into rs2 (and vice versa). The forwarded values must be chosen so using the stale register-file value would give the *opposite* branch outcome — otherwise a forwarding failure is undetectable.
- **JALR base forwarded distance-1 and distance-2**: produce the base register immediately before JALR and verify the correct target is reached.

---

## E. Jump/link semantics

- **JAL writes PC+4**: verified by existing test — keep.
- **JAL x0 (plain jump)**: `JAL x0, +8` — jump taken, x0 stays 0, link discarded.
- **Full call/return pattern**: `JAL x1, func; … func: JALR x0, 0(x1)` — verifies the return address is saved and restored correctly.
- **JALR LSB clearing**: base+imm odd (e.g. base=8, imm=5 → raw 13 → aligned 12); fetch must come from address 12.
- **JALR with negative immediate**: `JALR x5, -4(x1)` where x1 points past the target.

---

## F. Invalid encodings in branch/jump instructions

- **B-type with illegal funct3** (`010`, `011`): must raise `exception`.
- **JALR with funct3 ≠ `000`**: must raise `exception` (IDstage.scala:243).
- Both cases inside a **flushed slot**: `exception` must stay false (ties back to C.3).

---

## Organization

One `should`-clause per scenario group with a descriptive name:

```
"BLT and BLTU should disagree on (-1, 5) signed/unsigned boundary" should "work" in { … }
"Backward BNE loop should retire exactly N times" should "work" in { … }
"Wrong-path register write must not retire" should "work" in { … }
"Taken branch at branch target should flush twice" should "work" in { … }
"Distance-1 forwarding into rs1 vs rs2 of BEQ should both work" should "work" in { … }
```

Each test is self-contained (its own program binary), so a failure names the broken behavior directly rather than requiring waveform inspection.
