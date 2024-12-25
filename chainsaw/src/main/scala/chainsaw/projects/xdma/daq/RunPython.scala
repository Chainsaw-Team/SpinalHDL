package chainsaw.projects.xdma.daq

import java.io.{BufferedReader, InputStreamReader}

object RunPython {
  def apply(scriptName: String, args: Seq[String]): Boolean = {

    val pythonScript = f"./chainsaw-python/$scriptName"
    val pythonInterpreter = "./chainsaw-python/.venv/bin/python3"
    val pbArgs = Seq(pythonInterpreter, pythonScript) ++ args
    val pb = new ProcessBuilder(pbArgs: _*)
    pb.redirectErrorStream(true)

    try {
      // 尝试启动进程
      val process = pb.start()

      // 读取脚本输出
      val reader = new BufferedReader(new InputStreamReader(process.getInputStream))
      var line: String = null
      while ({
        line = reader.readLine()
        line != null
      }) {
        println(line)
      }

      // 正常处理完毕，返回 true
      true
    } catch {
      case e: Exception =>
        // 捕获运行时错误，例如无法执行脚本或进程问题
        false
    }
  }
}
