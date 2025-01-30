#!/bin/bash

module_name=ComponentDemodulator

current_dir="$(pwd)"

cd "$current_dir/simWorkspace/$module_name/xsim" || exit

if [ -f "$current_dir/${module_name}.wcfg" ]; then
  echo -e "open_wave_database ${module_name}.wdb\nopen_wave_config $current_dir/${module_name}.wcfg" > view_wave.tcl
else
  echo -e "open_wave_database ${module_name}.wdb" > view_wave.tcl
fi

$VIVADO_HOME/bin/vivado -source view_wave.tcl
