package com.zhangbaowei.reactive_demo.bizlib.nodes;

public class FlowLine extends BaseNode {

    private String to;

    public String getTo() {
        return to;
    }

    public FlowLine setTo(String to) {
        this.to = to;
        return this;
    }

    @Override
    public void execute(ExecutionContext execution) {
        throw new RuntimeException("FlowLine can not execute :" + this.getId());
    }

    @Override
    public void next(ExecutionContext executionContext) {
        throw new RuntimeException("FlowLine can not run :" + this.getId());
    }
}