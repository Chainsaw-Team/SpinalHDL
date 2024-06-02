package spinal.lib.eda

import spinal.core.HertzNumber

package object xilinx {

  // Xilinx Device Family Enum
  sealed trait XilinxDeviceFamily
  object UltraScale extends XilinxDeviceFamily
  object UltraScalePlus extends XilinxDeviceFamily
  object Series7 extends XilinxDeviceFamily

  // Vivado Task Enum
  sealed trait VivadoTaskType
  object PROGEN extends VivadoTaskType // create project
  object SYNTH extends VivadoTaskType
  object IMPL extends VivadoTaskType
  object BITGEN extends VivadoTaskType

  class XilinxDevice(
      val family: XilinxDeviceFamily,
      val part: String, // full name including family name, part name, package name, speed grade and temperature grade , e.g. xczu7ev-ffvc1156-2-e
      val fMax: HertzNumber, // recommended fmax, used as default value in synthesis/implementation
      val budget: VivadoUtil = VivadoUtil()
  ) {

    // on chip storage in bits
    private def bramCapacity: Double = budget.bram36 * 36 * 1024

    private def uramCapacity: Double = budget.uram288 * 288 * 1024

    def onChipStorage: Double = bramCapacity + uramCapacity

  }

}
