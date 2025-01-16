from Interface.InterfaceParser import InterfaceParser


class Axi4LiteParser(InterfaceParser):
    def __init__(self):
        super().__init__()
        self.identifier = "axi_lite"
        self.required_fields = [
            "araddr", "arprot", "arready", "arvalid",
            "awaddr", "awprot", "awready", "awvalid",
            "bready", "bresp", "bvalid",
            "rdata", "rready", "rresp", "rvalid",
            "wdata", "wready", "wstrb", "wvalid"
        ]
        self.optional_fields = [
            "arburst", "arcache", "arlen", "arlock", "arqos", "arsize",
            "awburst", "awcache", "awlen", "awlock", "awqos", "awsize",
            "rlast", "wlast"
        ]

    def get_definition(self, interface):
        addr = next(field for field in interface["fields"] if "addr" in field.field_name)
        addr_width = addr.bit_width
        direction = "master" if addr.direction == "Output" else "slave"
        data_width = next(field for field in interface["fields"] if "data" in field.field_name).bit_width
        interface_name = interface["name"]
        config = f"val {interface_name}_Config = AxiLite4Config(addressWidth = {addr_width}, dataWidth = {data_width})"
        return f"""
        {config}
        val {interface_name} = {direction}(AxiLite4({interface_name}_Config))
        {interface_name}.setNameForEda()
        """
