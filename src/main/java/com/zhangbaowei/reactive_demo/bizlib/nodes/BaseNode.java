package com.zhangbaowei.reactive_demo.bizlib.nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhangbaowei
 * @description:
 * @date 2021/7/20 9:10
 */
public abstract class BaseNode {
    private final String type = this.getClass().getSimpleName();
    private String id;
    /**
     * 显示名
     */
    private String displayName = "";
    private List<FlowLine> forwards = new ArrayList<>();

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<FlowLine> getForwards() {
        return forwards;
    }

    public void setForwards(List<FlowLine> forwards) {
        this.forwards = forwards;
    }

    public  abstract   void execute(ExecutionContext execution);

    public abstract void next(ExecutionContext executionContext);
}
