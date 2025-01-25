from Interface.InterfaceParser import InterfaceParser


class FlowParser(InterfaceParser):
    def __init__(self):
        super().__init__()
        self.identifier = ""
        self.required_fields = ["tvalid", "tdata"]
        self.optional_fields = ["tlast"]

    def get_definition(self, interface):
        data = next(field for field in interface["fields"] if field.field_name == "tdata")
        data_width = data.bit_width
        direction = "master" if data.direction == "Output" else "slave"
        interface_name = interface["name"]
        field_names = [field.field_name for field in interface["fields"]]
        if "tlast" in field_names:
            return f"  val {interface_name} = {direction}(Flow(Fragment(Bits({data_width} bits))))\n  {interface_name}.setNameForVivado()"
        else:
            return f"  val {interface_name} = {direction}(Flow(Bits({data_width} bits)))\n  {interface_name}.setNameForVivado()"
