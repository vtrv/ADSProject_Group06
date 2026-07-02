# Code Review Results — 05 Branch Instructions

## Overall Assessment

The branching implementation is **fundamentally correct** for basic execution. No critical bugs were found in the taken/not-taken branch, JAL/JALR, pipeline flush, or forwarding-into-branch paths. The control-hazard scheme matches the textbook design: branches resolve in EX, the redirect signal flushes exactly the two wrong-path slots (IF barrier + ID barrier), and the PC updates to the target in the same cycle.

---

## What Was Verified

### Flush scope and timing
On a taken branch or jump at cycle *c*, the IF barrier and ID barrier latch bubbles (`uop=NOP, rd=0, wrEn=0`), killing the two wrong-path instructions. The target instruction reaches EX exactly 2 cycles later. Bubbles carry no write enables, so they cannot redirect, write the register file, or appear as forwarding sources.

### Forwarding into branch and jump decisions
Branch comparisons and the JALR target use post-forwarding-mux operands — the muxes in `core.scala:131–149` feed into `EXstage.io.operandA/B`. Immediates are protected from bogus rs2 forwarding because ID zeroes `rs2Out` for non-R/B-type instructions (`IDstage.scala:105`), and JAL zeroes both source indices.

### Distance-3 hazard (write-then-read same cycle)
Covered by the same-cycle write-before-read bypass in the register file (`RegisterFile.scala:69–88`), which closes the gap that the two-deep forwarding unit cannot reach.

### JALR correctness
The LSB of the computed target is cleared per the RISC-V spec (`EXstage.scala:116`). The link address (PC+4) is muxed into `aluResult` for both JAL and JALR, so it writes back correctly and is available as a forwarding source.

### Invalid instructions cannot redirect
An invalid B-type funct3 (values 010 or 011) leaves `uop=NOP` while setting `XcptInvalid`; `redirect` and `wrEn` stay low. The two states are mutually exclusive by construction.

---

## Issues Found

### 1. Misaligned branch/jump targets are silently truncated (latent wrong-execution bug)

**Severity:** Medium — causes silent wrong execution, not a visible exception.

B-type and J-type immediates are multiples of 2, not 4. JALR can also produce any even address. `IFstage.scala:53` indexes memory as `IMem(PC >> 2)`, which silently drops bits [1:0]. A target of `0x16` executes the instruction at `0x14`, and all subsequent PC-relative targets are skewed.

**Fix:** Check `redirectTarget(1, 0) =/= 0.U` in EX and raise the exception (or assert). The RISC-V spec mandates an instruction-address-misaligned exception for this case.

### 2. `check_res` for a branch outputs a meaningless ALU result

**Severity:** Low — does not affect functional correctness, but makes tests brittle.

The WB stage unconditionally outputs `aluResult` as `check_res` regardless of `wrEn`. For branch instructions, the ALU defaults to ADD, so `check_res` is the sum of the two compare operands. The testbench relies on this (e.g., `BEQ x1, x2` where x1=x2=5 expects `10.U`). If the default ALU op ever changes, tests break for non-obvious reasons.

**Fix:** Gate `check_res` on `wrEn`, or document the quirk explicitly in the testbench.

### 3. Jump to uninitialized or out-of-bounds memory executes silently

**Severity:** Low — only matters if test programs contain bad jump targets.

Instruction word 0 is treated as a bubble (`IDstage.scala:248`), so a jump to zeroed memory loops on NOPs forever with no exception. There is no bounds check on PC against the 4096-word IMem. This is acceptable for the assignment scope.

### 4. JAL/JAR resolved in EX, not ID (performance, not correctness)

**Severity:** Informational — no impact on correctness for the current assignment.

JAL's target is known at decode but is resolved in EX, incurring a 2-cycle penalty instead of 1. This is worth noting before task 06 (BTB), where the baseline branch penalty is the starting point for the buffer's benefit calculation.
