import json

from pyverilog.vparser.parser import parse

import os

from Interface import *


# TODO: using dataclasses?
class ParserConfig:
    """
    Configuration class to manage global parser settings.
    """

    def __init__(self,
                 parse_interfaces: bool = True,
                 target_package_name: str = None):
        self.parse_interfaces = parse_interfaces  # Control interface parsing
        self.target_package_name = target_package_name  # Control interface parsing


# Global configuration object
parser_config = ParserConfig(parse_interfaces=True)

# Constants for eliminating magic values
EXCLUDE_DIR = ".cache"
XCI_EXT = ".xci"
VERILOG_EXT = ".v"
STUB_IDENTIFIER = "stub"


def find_files_by_extension(directory, extension, contains=None):
    """
    Helper function to find files with a specific extension in a directory.
    Optionally filters files that contain a specific substring.

    :param directory: The directory to search in.
    :param extension: File extension to look for (e.g., ".xci").
    :param contains: Filter files containing this substring.
    :return: List of file paths.
    """
    return [
        os.path.join(directory, f)
        for f in os.listdir(directory)
        if f.endswith(extension) and (contains is None or contains in f)
    ]


def scan_ip_location(ip_location_dir: str, target_dir: str):
    """
    Scans the IP directory, finds `.xci` and `.v` files, and generates Scala blackbox files.

    :param ip_location_dir: Directory containing IP files.
    :param target_dir: Directory to store generated Scala blackbox files.
    """
    for root, _, files in os.walk(ip_location_dir):
        if not root.endswith(EXCLUDE_DIR):
            # Extract relevant files
            xci_files = find_files_by_extension(root, XCI_EXT)
            stub_files = find_files_by_extension(root, VERILOG_EXT, contains=STUB_IDENTIFIER)

            # Process files
            for xci_file in xci_files:
                if len(stub_files) == 1 and EXCLUDE_DIR not in xci_file:
                    module_ios = extract_modules_and_ios(stub_files[0])
                    ip_name = os.path.splitext(os.path.basename(xci_file))[0]
                    scala_file_path = os.path.join(target_dir, f"{ip_name}.scala")
                    generate_blackbox(module_ios, scala_file_path, xci_file)


# TODO: 除了生成blackbox之外,还生成一个仅包含此blackbox的DUT,以方便测试
def convert_io_to_scala(io_dict, xci_file_path=None):
    """
    Converts module IO definitions to Scala code in SpinalHDL syntax.

    :param io_dict: Dictionary of module IO definitions.
    :param xci_file_path: Optional path to additional RTL.
    :return: Scala code as a string.
    """
    scala_code = ""
    parsers = [Ddr4Parser(), Axi4LiteParser(), Axi4StreamParser()]

    for module_name, signals in io_dict.items():
        scala_code += f"case class {module_name}() extends BlackBox {{\n"
        candidates = [IoDefinition(sig["direction"], sig["width"], name) for name, sig in signals.items()]

        # Generate interface definitions
        if parser_config.parse_interfaces:
            for parser in parsers:
                interfaces = parser.get_interface_groups(candidates)
                for interface in interfaces:
                    scala_code += parser.get_definition(interface) + "\n"
                    candidates = [c for c in candidates if c.name not in [f.name for f in interface["fields"]]]

        # Add remaining ports
        direction_map = {"Input": "in", "Output": "out"}
        for candidate in candidates:
            direction = direction_map[candidate.direction]
            if candidate.bit_width == 1:
                scala_code += f"  val {candidate.name} = {direction} Bool ()\n"
            else:
                scala_code += f"  val {candidate.name} = {direction} Bits ({candidate.bit_width} bits)\n"

        # Add optional RTL path
        if xci_file_path:
            scala_code += f"\n  addRTLPath(raw\"{xci_file_path}\")\n"
        scala_code += "}\n\n"
    return scala_code


def generate_blackbox(description, scala_file_path, xci_file_path: str = None):
    contents = []
    # 获取package信息
    if parser_config.target_package_name is not None:
        contents.append(f"package {parser_config.target_package_name}")

    # 进行import
    contents.append(
        """
import spinal.core._
import spinal.lib.bus.amba4.axis.Axi4Stream
import spinal.lib.bus.amba4.axis.Axi4StreamConfig
import spinal.lib._
        
        """
    )
    contents.append(convert_io_to_scala(description, xci_file_path))
    with open(scala_file_path, 'w') as f:
        f.write("\n".join(contents))


# TODO: 在blackbox parser的基础上制作一个方法扫描IP location下所有IP(基于.xci文件)并生成一个scala package-
def extract_modules_and_ios(file_path):
    """
    基于 pyverilog 的 AST 从 Verilog 文件中提取模块的详细信息,包括模块名及其IO信息(名字,方向和位宽)
    :param file_path: Verilog 文件路径
    :return: 包含模块信息的字典
    """
    print(f"parsing {file_path}...")

    io_types = ['Input', 'Output']  # TODO: 考虑inout的情况
    ast, _ = parse([file_path])  # get AST
    result = {}

    """
    视乎声明方式,IO可能通过portlist(SpinalHDL生成的.v)或items(Vivado IP生成的.v)获取
    通过portlist时,通过portlist中port的first|second获取
    通过items时,通过items中item的list中的元素获取
    """

    # Parse Verilog file and build AST
    for definition in ast.description.definitions:
        if definition.__class__.__name__ == 'ModuleDef':  # 找出module
            module_name = definition.name
            result[module_name] = {}

            def add_port(port):
                if 'lsb' in dir(port.width):
                    width = int(port.width.msb.value) - int(port.width.lsb.value) + 1
                else:
                    width = 1
                result[module_name][port.name] = {'direction': port.__class__.__name__, 'width': width}

            for item in definition.items:  # 在items中搜索IO
                if item.__class__.__name__ == 'Decl':
                    if 'list' in dir(item):
                        for port in item.list:
                            if port.__class__.__name__ in io_types:
                                add_port(port)
            if definition.portlist:  # 在portlist中搜索IO
                for port in definition.portlist.ports:
                    if 'first' in dir(port):
                        port = port.first
                        if port.__class__.__name__ in io_types:
                            add_port(port)

    return result


# 示例代码运行
if __name__ == "__main__":
    # modify global configuration
    parser_config.parse_interfaces = True
    parser_config.target_package_name = "chainsaw.projects.xdma.daq.ku060Ips"
    # conversion
    ip_location = "/home/ltr/SpinalHDL/KU060IP"  # your Vivado IP location
    scala_package_path = "/home/ltr/SpinalHDL/chainsaw/src/main/scala/chainsaw/projects/xdma/daq/ku060IPs"  # your Scala Package location
    scan_ip_location(ip_location, scala_package_path)
