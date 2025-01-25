from Interface.InterfaceParser import InterfaceParser


class Axi4StreamParser(InterfaceParser):
    def __init__(self):
        super().__init__()
        self.identifier = "axis"
        self.required_fields = ["tvalid", "tready", "tdata"]
        self.optional_fields = ["tlast", "tkeep", "tuser", "tid", "tdest"]

    def get_definition(self, interface):
        data = next(field for field in interface["fields"] if field.field_name == "tdata")
        assert data.bit_width % 8 == 0, f"Invalid AXI4-Stream bit width: {data.bit_width}"
        direction = "master" if data.direction == "Output" else "slave"

        interface_name = interface["name"]
        field_names = [field.field_name for field in interface["fields"]]
        axi_fields = ["tkeep", "tuser", "tid", "tdest"]  # these are fields that SpinalHDL Stream doesn't have
        use_axi = set(axi_fields) & set(field_names)
        if use_axi:
            interface_parameters = []
            data_width_in_bytes = data.bit_width // 8
            interface_parameters.append(f"dataWidth = {data_width_in_bytes}")
            if any(field.field_name == "tlast" for field in interface["fields"]):
                interface_parameters.append("useLast = true")
            if any(field.field_name == "tkeep" for field in interface["fields"]):
                interface_parameters.append("useKeep = true")
            if any(field.field_name == "tuser" for field in interface["fields"]):
                interface_parameters.append("useUser = true")
            tid = next((field for field in interface["fields"] if field.field_name == "tid"), None)
            if tid:
                interface_parameters.append(f"useId = true, idWidth = {tid.bit_width}")
            tdest = next((field for field in interface["fields"] if field.field_name == "tdest"), None)
            if tdest:
                interface_parameters.append(f"useDest = true, destWidth = {tdest.bit_width}")

            config = f"val {interface_name}_Config = Axi4StreamConfig({', '.join(interface_parameters)})"
            return f"""
            {config}
            val {interface_name} = {direction}(Axi4Stream({interface_name}_Config))
            {interface_name}.setNameForEda()
            """
        else:
            if "tlast" in field_names:
                return f"  val {interface_name} = {direction}(Stream(Fragment(Bits({data.bit_width} bits))))\n  {interface_name}.setNameForVivado()"
            else:
                return f"  val {interface_name} = {direction}(Stream(Bits({data.bit_width} bits)))\n  {interface_name}.setNameForVivado()"


