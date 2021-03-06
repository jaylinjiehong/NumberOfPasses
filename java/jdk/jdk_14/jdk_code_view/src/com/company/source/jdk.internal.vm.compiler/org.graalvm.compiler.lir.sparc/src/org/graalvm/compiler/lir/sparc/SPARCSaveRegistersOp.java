/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.lir.sparc;

import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;
import static org.graalvm.compiler.lir.sparc.SPARCDelayedControlTransfer.DUMMY;

import jdk.internal.vm.compiler.collections.EconomicSet;
import org.graalvm.compiler.asm.sparc.SPARCAddress;
import org.graalvm.compiler.asm.sparc.SPARCMacroAssembler;
import org.graalvm.compiler.lir.LIRInstructionClass;
import org.graalvm.compiler.lir.Opcode;
import org.graalvm.compiler.lir.StandardOp.SaveRegistersOp;
import org.graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.sparc.SPARC;

/**
 * Saves registers to stack slots.
 */
@Opcode("SAVE_REGISTER")
public class SPARCSaveRegistersOp extends SaveRegistersOp implements SPARCLIRInstructionMixin {
    public static final LIRInstructionClass<SPARCSaveRegistersOp> TYPE = LIRInstructionClass.create(SPARCSaveRegistersOp.class);
    public static final Register RETURN_REGISTER_STORAGE = SPARC.d62;
    public static final SizeEstimate SIZE = SizeEstimate.create(32);
    private final SPARCLIRInstructionMixinStore store;

    /**
     *
     * @param savedRegisters the registers saved by this operation which may be subject to
     *            {@linkplain #remove(EconomicSet) pruning}
     * @param savedRegisterLocations the slots to which the registers are saved
     */
    public SPARCSaveRegistersOp(Register[] savedRegisters, AllocatableValue[] savedRegisterLocations) {
        super(TYPE, savedRegisters, savedRegisterLocations);
        this.store = new SPARCLIRInstructionMixinStore(SIZE);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb) {
        SPARCMacroAssembler masm = (SPARCMacroAssembler) crb.asm;
        // Can be used with VIS3
        // new Movxtod(SPARC.i0, RETURN_REGISTER_STORAGE).emit(masm);
        // We abuse the first stackslot for transferring i0 to return_register_storage
        // assert slots.length >= 1;
        SPARCAddress slot0Address = (SPARCAddress) crb.asAddress(slots[0]);
        masm.stx(SPARC.i0, slot0Address);
        masm.lddf(slot0Address, RETURN_REGISTER_STORAGE);

        // Now save the registers
        for (int i = 0; i < savedRegisters.length; i++) {
            if (savedRegisters[i] != null) {
                assert isStackSlot(slots[i]) : "not a StackSlot: " + slots[i];
                Register savedRegister = savedRegisters[i];
                StackSlot slot = asStackSlot(slots[i]);
                SPARCAddress slotAddress = (SPARCAddress) crb.asAddress(slot);
                RegisterValue input = savedRegister.asValue(slot.getValueKind());
                SPARCMove.emitStore(input, slotAddress, slot.getPlatformKind(), DUMMY, null, crb, masm);
            }
        }
    }

    @Override
    public SPARCLIRInstructionMixinStore getSPARCLIRInstructionStore() {
        return store;
    }
}
