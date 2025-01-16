class IoDefinition:
    """描述端口的类"""

    def __init__(self, direction, bit_width, name, interface_name=None, field_name=None):
        self.direction = direction
        self.bit_width = bit_width
        self.name = name
        self.interface_name = interface_name  # 所属的接口名
        self.field_name = field_name  # 字段类型


class InterfaceParser:
    """
    基础接口解析器，用于提取端口并生成接口声明。
    """

    def __init__(self):
        self.identifier = ""
        self.required_fields = []
        self.optional_fields = []

    @property
    def all_fields(self):
        """返回所有可能的字段（必需 + 可选）"""
        return self.required_fields + self.optional_fields

    def get_interface_groups(self, candidates):
        """
        提取符合接口规则的端口组（接口）。

        :param candidates: List of IoDefinition
        :return: List of Interface
        """
        from collections import defaultdict

        # 分组，根据端口名匹配接口
        grouped_interfaces = defaultdict(list)
        for candidate in candidates:
            io_name = candidate.name
            if self.identifier.lower() not in io_name.lower():
                continue  # 必须包含 identifier
            # 必须以某个字段名称结尾
            for field in self.all_fields:
                if io_name.endswith(f"_{field}"):
                    interface_name = io_name.replace(f"_{field}", "")
                    grouped_interfaces[interface_name].append(
                        IoDefinition(candidate.direction, candidate.bit_width, candidate.name, interface_name, field)
                    )
                    break

        # 过滤掉没有满足 required_fields 的组
        valid_interfaces = []
        for interface_name, fields in grouped_interfaces.items():
            field_names = {field.field_name for field in fields}
            if all(field in field_names for field in self.required_fields):  # 必须包含所有必需字段
                valid_interfaces.append({"name": interface_name, "fields": fields})

        return valid_interfaces

    def get_definition(self, interface):
        """
        针对具体接口定义的生成逻辑（需由子类实现）。
        """
        raise NotImplementedError
