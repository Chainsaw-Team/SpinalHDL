package chainsaw.projects.xdma.daq

import java.io.{DataOutputStream, FileOutputStream}

object SaveBinFile {
  def apply(fileName: String, data: Seq[Int]): Unit = {
    val fos = new FileOutputStream(fileName)
    val dos = new DataOutputStream(fos)
    try {
      data.foreach(dos.writeInt)
    } finally {
      dos.close()
      fos.close()
    }
  }
}
