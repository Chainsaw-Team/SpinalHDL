cd || exit
module_name=ComponentDemodulator
cd ~/SpinalHDL/simWorkspace/$module_name/xsim || exit
echo -e "open_wave_database ${module_name}.wdb\nopen_wave_config /home/ltr/SpinalHDL/${module_name}.wcfg" > view_wave.tcl
vivado -source view_wave.tcl