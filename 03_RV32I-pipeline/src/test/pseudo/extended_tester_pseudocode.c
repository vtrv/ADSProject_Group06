// ==========================================================================
// Extended Test: I-type instructions, hazards, corner cases, exceptions
// ==========================================================================

// --- Fill pipeline (5 cycles) ---
wait_cycles(5);

// ================================================================
// Section 1: I-type Immediate Instructions (negative/boundary)
// ================================================================
x1 = 0 - 1;                         assert(result == 0xFFFFFFFF);
x2 = 0 - 2048;                      assert(result == 0xFFFFF800);
x3 = 0 + 2047;                      assert(result == 2047);

// 3 NOPs to allow x1 to reach the register file safely
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

x4 = x1 ^ 0xFF;                     assert(result == 0xFFFFFF00);
x5 = 0 | 0x123;                     assert(result == 0x00000123);
x6 = x1 & 0xFF;                     assert(result == 0x000000FF);
x7 = (signed)x1 < 0 ? 1 : 0;        assert(result == 1);
x8 = (unsigned)x1 < 1 ? 1 : 0;      assert(result == 0);

// ================================================================
// Section 2: Shift Immediate Edge Cases (shift by 0 and 31)
// ================================================================
x9 = 0 - 1;                         assert(result == 0xFFFFFFFF);

// 3 NOPs to allow x9 to reach the register file safely
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

x10 = x9 << 0;                      assert(result == 0xFFFFFFFF); // No shift
x11 = x9 << 31;                     assert(result == 0x80000000); 
x12 = (unsigned)x9 >> 0;            assert(result == 0xFFFFFFFF); // Logical, no shift
x13 = (unsigned)x9 >> 31;           assert(result == 1);          // Logical
x14 = (signed)x9 >> 0;              assert(result == 0xFFFFFFFF); // Arithmetic, no shift
x15 = (signed)x9 >> 31;             assert(result == 0xFFFFFFFF); // Arithmetic (all sign bits)

// ================================================================
// Section 3: RAW Hazard — Distance 1 (back-to-back)
// Multi-cycle would give x17=42; pipeline gives x17=0 (stale x16)
// ================================================================
x16 = 0 + 42;                       assert(result == 42);
x17 = stale_x16 + 0;                assert(result == 0);   // HAZARD: x16 stale = 0

// Clear pipeline after hazard
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

// ================================================================
// Section 4: WAW — Two Writes to Same Register
// Second write (222) wins; consumer reads after both complete
// ================================================================
x22_attempt1 = 0 + 111;             assert(result == 111);
x22_attempt2 = 0 + 222;             assert(result == 222);

// 3 NOPs to allow BOTH writes to clear the pipeline
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

x23 = x22 + 0;                      assert(result == 222); // x22 = 222 (2nd write won)

// ================================================================
// Section 5: x0 Hardwiring (write to x0 must be discarded)
// ================================================================
x0_attempt = 0 + 999;               assert(result == 999); // ALU computes 999, but x0 stays 0

// 3 NOPs to wait out the writeback stage
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

x24 = x0 + x0;                      assert(result == 0);   // x0 is still 0

// ================================================================
// Section 6: Self-Referencing Operation (same reg as src & dst)
// ================================================================
x25 = 0 + 10;                       assert(result == 10);

// 3 NOPs for x25 to safely reach the register file
nop();                              assert(result == 0);
nop();                              assert(result == 0);
nop();                              assert(result == 0);

x25 = x25 + x25;                    assert(result == 20);  // 10 + 10 = 20