from Interface.InterfaceParser import InterfaceParser


class Axi4StreamParser(InterfaceParser):
    def __init__(self):
        super().__init__()
        self.identifier = "axis"
        self.required_fields = ["tvalid", "tready", "tdata"]
        self.optional_fields = ["tlast", "tkeep", "tuser", "tid", "tdest"]

    def get_definition(self, interface):
        fields = []
        data = next(field for field in interface["fields"] if field.field_name == "tdata")
        assert data.bit_width % 8 == 0, f"Invalid AXI4-Stream bit width: {data.bit_width}"
        data_width_in_bytes = data.bit_width // 8
        direction = "master" if data.direction == "Output" else "slave"
        fields.append(f"dataWidth = {data_width_in_bytes}")
        if any(field.field_name == "tlast" for field in interface["fields"]):
            fields.append("useLast = true")
        if any(field.field_name == "tkeep" for field in interface["fields"]):
            fields.append("useKeep = true")
        if any(field.field_name == "tuser" for field in interface["fields"]):
            fields.append("useUser = true")
        tid = next((field for field in interface["fields"] if field.field_name == "tid"), None)
        if tid:
            fields.append(f"useId = true, idWidth = {tid.bit_width}")
        tdest = next((field for field in interface["fields"] if field.field_name == "tdest"), None)
        if tdest:
            fields.append(f"useDest = true, destWidth = {tdest.bit_width}")
        interface_name = interface["name"]
        config = f"val {interface_name}_Config = Axi4StreamConfig({', '.join(fields)})"
        return f"""
        {config}
        val {interface_name} = {direction}(Axi4Stream({interface_name}_Config))
        {interface_name}.setNameForEda()
        """
