# private-notes 私人注释
点一下star ✨,是对作者最大的支持

## 项目来源

本仓库基于原始项目 **Private Notes** 进行二次开发与功能增强。

- 原始仓库：[https://gitee.com/The-Blind/private-notes](https://gitee.com/The-Blind/private-notes)
- 原作者：TheBlind
- 当前仓库定位：在保留原有私人注释能力的基础上，继续补充 IntelliJ IDEA 2025.x 兼容性修复、交互优化与增强功能

### 当前仓库已新增/增强的能力

- 适配 IntelliJ IDEA 2025.x
- 修复输入法候选框无法正常弹出的问题
- 修复设置页无法打开的问题
- 优化注释悬浮查看体验，支持滚动、复制、选中
- 支持悬浮查看完整注释内容
- 增强注释存储兼容性与迁移能力

### 致谢与说明

如果你准备将当前仓库同步到 GitHub，建议保留以上原项目引用信息，并同时保留仓库中的 [LICENSE](./LICENSE) 文件。

#### 介绍

 你还在为项目中不敢添加 "敏感注释"!<br>

 源码是只读文件不能添加注释而烦恼吗？<br>

 在任何你想加注释的地方 按下**Alt + Enter**鼠标移出点击即可保存<br>

 已有私人注释 按下**Alt + Enter**即可快速编辑<br>
 
 **Alt + p** 可快速添加或者编辑私人注释
 
 **Alt + o** 展示私人注释的其它操作
 
 右键菜单 选择 **私人注释** 查看操作
 
      
![示例1](./doc/show.gif)

#### 安装
打开IntelliJ IDEA -> plugin，搜索 Private Notes，安装重启即可

#### 配置
![输入图片说明](https://images.gitee.com/uploads/images/2021/1028/165110_4fadd758_2189193.png "屏幕截图.png")


#### 同步
私人注释 都缓存在 当前用户目录下的 .privateNotes文件夹中,如需同步，可以借助强大的Git

![示例1](./doc/localFile.jpg)
如果你熟练git命令可以直接通过 命令的方式，或者通过idea像我们平时一样

将项目导入idea中,并上传到gitee或者github中
[将项目首次导入到 gitee中](https://blog.csdn.net/qq_40495860/article/details/102722894)

当你完成第一次上传时,即可使用右键 **私人注释** 中的Git操作 完成快速的 Pull和CommitAndPush操作
<br/>
**注意**：必须 使用Git命令或者Idea 完成第一次提交

