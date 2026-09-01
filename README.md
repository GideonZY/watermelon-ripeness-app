# 西瓜听诊器 AndroidApp

**本项目所有代码由ChatGPT完成，我完全不动代码。**

这是一个教学型、可运行的 MVP：录制 2.5 秒单声道 16 kHz PCM，在内存中计算主频、频谱质心、RMS 能量和前后段衰减，再用透明的占位规则输出“偏生 / 适熟 / 偏熟”。v0.2.0 增加了“生—熟—过熟”指针仪表盘并取消检测录音落盘；v0.2.1 在此基础上增加 100 ms 实时频率刷新。**当前判断规则没有经过真实数据验证，不能用于商业分级或食品质量保证。**


## v0.2.1 实时频率显示

- 点击“开始检测”后立即进入实时仪表盘。
- 麦克风每 100 ms 分析一次当前 PCM 帧并刷新主频与指针。
- 低能量环境噪声不会推动指针；有效频率采用轻度指数平滑减少抖动。
- 完整检测结束后仍给出最终成熟度结果。
- 音频全程仅保存在内存，不生成 WAV、不写入手机存储。

## 第一次运行（零基础步骤）

1. 安装最新版 Android Studio，安装向导中保留 Android SDK、Platform Tools 和 Emulator 的默认选项。
2. 打开 Android Studio，选择 **Open**，选中本项目最外层 `WatermelonRipenessMVP` 文件夹。
3. 等待右下角 Gradle 同步完成。若提示安装 Android SDK 34，点击安装。
4. 安卓手机打开“开发者选项”和“USB 调试”，USB 连接电脑并在手机上允许调试。
5. Android Studio 顶部选择你的手机，点击绿色运行三角形。首次点击“开始检测”时允许麦克风权限。

项目包含 Gradle 版本配置，但没有附带二进制 `gradle-wrapper.jar`。Android Studio 通常会使用内置 Gradle 完成同步；若它提示缺少 wrapper，请在欢迎页设置中选择 Gradle 8.2.1，或在 Android Studio 终端执行一次 `gradle wrapper --gradle-version 8.2.1`。

从 v0.2.0 开始，普通检测不会生成或保存 WAV 文件。录音 PCM 只在本次检测期间保存在内存中，完成特征提取和判断后由系统回收。若未来需要采集训练样本，应单独增加显式的“保存样本”模式。

## 使用方法

- 环境尽量安静；手机麦克风距瓜表约 10 cm，位置和角度保持一致。
- 点击“开始检测”后，在 2.5 秒内用同一根手指、相近力度拍 2～3 次。
- 每个瓜建议在不同位置检测 3 次；不要拿同一次录音重复充数。普通检测不会保留录音，如需正式采集训练数据，应使用后续单独的数据采集版本。

## 样本采集与标注

建议先做小试验 60 个瓜，每个瓜 3 条录音，三类各约 20 个瓜；可行后扩展到至少 300～1000 个瓜。训练集、验证集和测试集必须按“瓜”划分，不能按录音随机划分，否则同一个瓜的声音会泄漏到测试集。

标签应在切瓜后确定。优先组合：中心及边缘糖度计 Brix、果肉硬度、空心/糠心情况、多人盲品。建议约定：

- `underripe`：硬、甜度低、香气不足；
- `ripe`：甜度和口感达到目标、无明显糠心；
- `overripe`：明显发绵、糠心、内部劣变或过熟口感。

不同品种、产地和季节的阈值可能不同，务必记录这些字段。CSV 模板位于 `data/samples_template.csv`，供后续专门的数据采集版本使用；`sample_id` 应表示“瓜 ID”，同一瓜的三条录音使用相同 ID 或增加独立 `recording_id` 字段。

## 从规则升级到 TensorFlow Lite

推荐训练流程：读取 WAV → 去直流/归一化 → 截取敲击事件 → 转 64 或 80 维 log-mel 频谱 → 数据增强（背景噪声、轻微增益、时间平移）→ 训练小型 CNN → 按瓜分组交叉验证 → 导出 `.tflite` 并做整数量化。

第一版可先用 CSV 中四个特征训练逻辑回归、随机森林或 XGBoost，建立可解释基线；再和 log-mel CNN 比较。核心指标使用 macro F1、三类召回率和混淆矩阵，不要只看准确率。测试集应包含训练时没见过的瓜、手机型号和采集日期。

App 内已经定义 `RipenessClassifier` 接口。训练完成后：

1. 添加 TensorFlow Lite Android 依赖；
2. 把模型放入 `app/src/main/assets/watermelon_model.tflite`；
3. 在 `TFLiteRipenessClassifier` 中实现同训练阶段完全一致的预处理和推理；
4. 将 `MainActivity` 的 `RuleBasedClassifier()` 换成 `TFLiteRipenessClassifier()`。

## 生成 APK

调试 APK：Android Studio 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。完成后点击通知中的 **locate**，文件通常在 `app/build/outputs/apk/debug/app-debug.apk`。

正式发布：选择 **Build → Generate Signed Bundle / APK**，优先选择 Android App Bundle（Google Play）；若要直接发给测试者则选 APK。创建并妥善备份 keystore，填写版本信息后选择 release。签名密钥丢失会影响后续更新。

## 已知限制

- 占位阈值只用于打通流程；手机麦克风、拍击力度、瓜品种都会显著影响结果。
- MVP 每次分析整段录音，并以最响窗口估算频谱；正式模型应检测并分别汇总多次敲击。
- `UNPROCESSED` 音源在部分手机上可能由系统回退或表现不同，需要真机测试。
