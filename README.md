# 芙莉莲桌面小精灵 (Frieren Desktop Pet)

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Language-Java25-orange.svg)
![JavaFX](https://img.shields.io/badge/Framework-JavaFX-green.svg)

这是一个基于 **JavaFX** 开发的开源桌面项目。本项目初衷是为了在学习 **Java 多线程、网络编程（RESTful API）以及系统级底层接口（JMX）** 的应用实践。这是我第一个JavaFX项目。

通过这个项目，我把芙莉莲带到了桌面，并赋予了她实用的“魔法”功能。

---

## ✨ 核心功能

* **交互式动画**：支持桌面自由拖拽、双击互动。
* **智能台词系统**：根据当前系统时间（清晨、深夜等）自动切换动态台词。
* **闲置提醒逻辑**：当检测到用户长时间未操作鼠标（发呆）时，芙莉莲会主动弹出气泡进行提醒。
* **硬件监控看板 (HUD)**：
    * 实时监控系统 **CPU 占用率** 与 **物理内存 (RAM)** 消耗。
    * 具备“魔法过载”预警：当 CPU 负载过高时自动触发警告。
* **剪贴板翻译魔法**：
    * 集成 **百度翻译 API**。
    * 一键读取剪贴板内容并进行中英互译，采用异步请求技术，确保 UI 界面永不卡顿。

---

## 🛠️ 技术栈

* **语言**：Java 25
* **图形库**：JavaFX (OpenJFX)
* **构建工具**：Maven
* **数据解析**：Jackson (JSON 处理)
* **硬件接口**：JMX (OperatingSystemMXBean)
* **版本控制**：Git / GitHub

---
