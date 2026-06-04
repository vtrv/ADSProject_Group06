// ADS I Class Project
// Assignment 02: Arithmetic Logic Unit and UVM Testbench
//
// Chair of Electronic Design Automation, RPTU University Kaiserslautern-Landau
// File created on 09/21/2025 by Tharindu Samarakoon (gug75kex@rptu.de)
// File updated on 10/31/2025 by Tobias Jauch (tobias.jauch@rptu.de)

`include "uvm_macros.svh"
import uvm_pkg::*;
import alu_tb_config_pkg::*;

class alu_seq_item extends uvm_sequence_item;

    // --------------------------------------------------------
    // Fields
    // --------------------------------------------------------
    // rand: the randomisation engine will fill these each time
    // .randomize() is called by the sequence.

    rand logic [DATA_WIDTH-1:0] operandA;   // first ALU operand
    rand logic [DATA_WIDTH-1:0] operandB;   // second ALU operand
    rand ALUOp                  operation;  // which operation to perform
         logic [DATA_WIDTH-1:0] aluResult;  // captured from DUT (not randomised)

    // --------------------------------------------------------
    // Factory registration
    // --------------------------------------------------------
    // The `uvm_object_utils macro registers this class with the
    // UVM factory so it can be created with ::type_id::create().
    `uvm_object_utils_begin(alu_seq_item)
        `uvm_field_int(operandA,  UVM_DEFAULT)
        `uvm_field_int(operandB,  UVM_DEFAULT)
        `uvm_field_enum(ALUOp, operation, UVM_DEFAULT)
        `uvm_field_int(aluResult, UVM_DEFAULT)
    `uvm_object_utils_end

    // --------------------------------------------------------
    // Constraint: operation must be a valid ALUOp value
    // --------------------------------------------------------
    // Without this constraint the randomiser could pick any
    // 8-bit value, including undefined opcodes.
    constraint valid_operation_c {
        operation inside {ADD, SUB, AND, OR, XOR,
                          SLL, SRL, SRA, SLT, SLTU, PASSB};
    }

    virtual function string convert2str();
        return $sformatf("operandA: 0x%0x, operandB: 0x%0x, operation: %0p, aluResult: 0x%0x",
                          operandA, operandB, operation, aluResult);
    endfunction

    function new(string name = "alu_seq_item"); 
        super.new(name);
    endfunction   

endclass
