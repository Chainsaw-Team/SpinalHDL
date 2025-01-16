from Interface.InterfaceParser import InterfaceParser


class Ddr4Parser(InterfaceParser):
    def __init__(self):
        super().__init__()
        self.identifier = "ddr4"
        self.required_fields = [
            "act_n", "adr", "ba", "bg", "ck_c", "ck_t", "cke", "cs_n",
            "dm_n", "dq", "dqs_c", "dqs_t", "odt", "reset_n"
        ]
        self.optional_fields = []

    def get_definition(self, interface):
        dqs = next(field for field in interface["fields"] if field.field_name == "dqs_c")
        data_width_in_byte = dqs.bit_width
        interface_name = interface["name"]
        return f"  val {interface_name} = Ddr4Interface(17, {data_width_in_byte})"
