/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
 *
 *
 *
 */
package jdk.incubator.foreign;

import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A padding layout. A padding layout specifies the size of extra space which is typically not accessed by applications,
 * and is typically used for aligning member layouts around word boundaries.
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code PaddingLayout} may have unpredictable results and should be avoided.
 * The {@code equals} method should be used for comparisons.
 *
 * @implSpec
 * This class is immutable and thread-safe.
 */
/* package-private */ final class PaddingLayout extends AbstractLayout implements MemoryLayout {

    PaddingLayout(long size) {
        this(size, size, Optional.empty());
    }

    PaddingLayout(long size, long alignment, Optional<String> name) {
        super(OptionalLong.of(size), alignment, name);
    }

    @Override
    public String toString() {
        return decorateLayoutString("x" + bitSize());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof PaddingLayout)) {
            return false;
        }
        PaddingLayout p = (PaddingLayout)other;
        return bitSize() == p.bitSize();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bitSize());
    }

    @Override
    PaddingLayout dup(long alignment, Optional<String> name) {
        return new PaddingLayout(bitSize(), alignment, name);
    }

    @Override
    public Optional<DynamicConstantDesc<MemoryLayout>> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "padding",
                CD_LAYOUT, MH_PADDING, bitSize()));
    }

    //hack: the declarations below are to make javadoc happy; we could have used generics in AbstractLayout
    //but that causes issues with javadoc, see JDK-8224052

    /**
     * {@inheritDoc}
     */
    @Override
    public PaddingLayout withName(String name) {
        return (PaddingLayout)super.withName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaddingLayout withBitAlignment(long alignmentBits) {
        return (PaddingLayout)super.withBitAlignment(alignmentBits);
    }
}
