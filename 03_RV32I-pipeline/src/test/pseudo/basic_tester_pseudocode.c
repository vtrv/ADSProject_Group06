// ==========================================================================
// Setup: Fill the 5-stage pipeline before executing tests
// ==========================================================================
wait_cycles(5);

nop();                                     assert(result == 0);

// ==========================================================================
// Basic Addition & Hazard Prevention
// ==========================================================================
x1 = 0 + 4;                                assert(result == 4);
x2 = 0 + 5;                                assert(result == 5);

// 3 NOPs to allow x1 and x2 to reach the register file safely
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);

// Safe to read x1 and x2
x3 = x1 + x2;                              assert(result == 9);

// ==========================================================================
// Subtraction & Hazard Prevention
// ==========================================================================
x4 = 0 + 2047;                             assert(result == 2047);
x5 = 0 + 16;                               assert(result == 16);

// 3 NOPs to allow x4 and x5 to reach the register file safely
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);

// Safe to read x4 and x5
x6 = x4 - x5;                              assert(result == 2031);

// ==========================================================================
// Bitwise Operations
// ==========================================================================
// 3 NOPs to allow x6 to reach the register file safely
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);
nop();                                     assert(result == 0);

// Safe to read x6 (and x3/x5 which finished long ago)
x7 = x6 ^ x3;                              assert(result == 2022);
x8 = x6 | x5;                              assert(result == 2047);
x9 = x6 & x5;                              assert(result == 0);

// ==========================================================================
// Shift Operations
// ==========================================================================
// Only 1 NOP needed here! x8 and x9 act as the first two delays for x7.
nop();                                     assert(result == 0);

// Safe to read x7 (and x2 which finished long ago)
x10 = x7 << x2;                            assert(result == 64704);
x11 = (unsigned)x7 >> x2;                  assert(result == 63); // Logical Shift Right
x12 = (signed)x7 >> x2;                    assert(result == 63); // Arithmetic Shift Right

// ==========================================================================
// Set Less Than (SLT / SLTU) Comparisons
// No NOPs needed here because x4 and x5 were computed a long time ago.
// ==========================================================================
// Signed comparisons
x13 = (signed)x4 < (signed)x4 ? 1 : 0;     assert(result == 0);
x13 = (signed)x4 < (signed)x5 ? 1 : 0;     assert(result == 0); // 2047 < 16 is false
x13 = (signed)x5 < (signed)x4 ? 1 : 0;     assert(result == 1); // 16 < 2047 is true

// Unsigned comparisons
x13 = (unsigned)x4 < (unsigned)x4 ? 1 : 0; assert(result == 0);
x13 = (unsigned)x4 < (unsigned)x5 ? 1 : 0; assert(result == 0);
x13 = (unsigned)x5 < (unsigned)x4 ? 1 : 0; assert(result == 1);

// ==========================================================================
// Exception Testing
// ==========================================================================
// DIV is not part of the basic RV32I integer instruction set.
execute(0x021141B3);                       // DIV x3, x2, x1
                                           assert(exception == true);