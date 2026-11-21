package com.qianxun.homestay.modules.portrait.entity;

/** 顾客画像可选标签字典表映射。 */
public class PortraitTagEntity {

    /** 标签主键。 */
    private Long id;
    /** 管理员可供画像选择的标签名称。 */
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
