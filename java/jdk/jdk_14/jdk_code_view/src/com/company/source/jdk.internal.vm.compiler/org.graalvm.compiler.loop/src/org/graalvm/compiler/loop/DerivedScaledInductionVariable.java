/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.compiler.loop;

import static org.graalvm.compiler.loop.MathUtil.mul;

import org.graalvm.compiler.core.common.type.IntegerStamp;
import org.graalvm.compiler.core.common.type.Stamp;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.calc.IntegerConvertNode;
import org.graalvm.compiler.nodes.calc.NegateNode;
import org.graalvm.compiler.nodes.util.GraphUtil;

public class DerivedScaledInductionVariable extends DerivedInductionVariable {

    private final ValueNode scale;
    private final ValueNode value;

    public DerivedScaledInductionVariable(LoopEx loop, InductionVariable base, ValueNode scale, ValueNode value) {
        super(loop, base);
        this.scale = scale;
        this.value = value;
    }

    public DerivedScaledInductionVariable(LoopEx loop, InductionVariable base, NegateNode value) {
        super(loop, base);
        this.scale = ConstantNode.forIntegerStamp(value.stamp(NodeView.DEFAULT), -1, value.graph());
        this.value = value;
    }

    public ValueNode getScale() {
        return scale;
    }

    @Override
    public ValueNode valueNode() {
        return value;
    }

    @Override
    public Direction direction() {
        Direction baseDirection = base.direction();
        if (baseDirection == null) {
            return null;
        }
        Stamp stamp = scale.stamp(NodeView.DEFAULT);
        if (stamp instanceof IntegerStamp) {
            IntegerStamp integerStamp = (IntegerStamp) stamp;
            if (integerStamp.isStrictlyPositive()) {
                return baseDirection;
            } else if (integerStamp.isStrictlyNegative()) {
                return baseDirection.opposite();
            }
        }
        return null;
    }

    @Override
    public ValueNode initNode() {
        return mul(graph(), base.initNode(), scale);
    }

    @Override
    public ValueNode strideNode() {
        return mul(graph(), base.strideNode(), scale);
    }

    @Override
    public boolean isConstantInit() {
        return scale.isConstant() && base.isConstantInit();
    }

    @Override
    public boolean isConstantStride() {
        return scale.isConstant() && base.isConstantStride();
    }

    @Override
    public long constantInit() {
        return base.constantInit() * scale.asJavaConstant().asLong();
    }

    @Override
    public long constantStride() {
        return base.constantStride() * scale.asJavaConstant().asLong();
    }

    @Override
    public ValueNode extremumNode(boolean assumeLoopEntered, Stamp stamp) {
        return mul(graph(), base.extremumNode(assumeLoopEntered, stamp), IntegerConvertNode.convert(scale, stamp, graph(), NodeView.DEFAULT));
    }

    @Override
    public ValueNode exitValueNode() {
        return mul(graph(), base.exitValueNode(), scale);
    }

    @Override
    public boolean isConstantExtremum() {
        return scale.isConstant() && base.isConstantExtremum();
    }

    @Override
    public long constantExtremum() {
        return base.constantExtremum() * scale.asJavaConstant().asLong();
    }

    @Override
    public void deleteUnusedNodes() {
        GraphUtil.tryKillUnused(scale);
    }

    @Override
    public String toString() {
        return String.format("DerivedScaleInductionVariable base (%s) %s %s", base, value.getNodeClass().shortName(), scale);
    }
}
