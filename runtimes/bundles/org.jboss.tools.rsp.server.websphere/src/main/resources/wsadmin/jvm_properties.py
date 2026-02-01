import json
import sys

MARKER_START = "RSP_JSON_START"
MARKER_END = "RSP_JSON_END"


def emit_json(payload):
    print(MARKER_START)
    print(json.dumps(payload))
    print(MARKER_END)


def split_list(list_str):
    if list_str is None:
        return []
    s = list_str.strip()
    if s.startswith("[") and s.endswith("]"):
        s = s[1:-1].strip()
    if not s:
        return []
    return s.split()


def resolve_server(server_name, node_name):
    if server_name:
        if node_name:
            server = AdminConfig.getid("/Node:%s/Server:%s/" % (node_name, server_name))
            if server:
                return server
        for candidate in AdminConfig.list("Server").splitlines():
            try:
                if AdminConfig.showAttribute(candidate, "name") == server_name:
                    return candidate
            except:
                pass
    servers = AdminConfig.list("Server").splitlines()
    return servers[0] if servers else None


def resolve_jvm(server):
    jvms = split_list(AdminConfig.list("JavaVirtualMachine", server))
    return jvms[0] if jvms else None


def list_props(jvm):
    props = split_list(AdminConfig.showAttribute(jvm, "systemProperties"))
    result = []
    for prop in props:
        name = AdminConfig.showAttribute(prop, "name") or ""
        value = AdminConfig.showAttribute(prop, "value") or ""
        required_raw = AdminConfig.showAttribute(prop, "required")
        required = True if str(required_raw).lower() == "true" else False
        result.append({"name": name, "value": value, "required": required})
    return result


def apply_props(jvm, entries):
    props = split_list(AdminConfig.showAttribute(jvm, "systemProperties"))
    for prop in props:
        AdminConfig.remove(prop)
    for entry in entries:
        if entry is None:
            continue
        name = entry.get("name") if isinstance(entry, dict) else None
        if not name:
            continue
        value = entry.get("value") if isinstance(entry, dict) else ""
        required = entry.get("required") if isinstance(entry, dict) else False
        attrs = [["name", name], ["value", value], ["required", "true" if required else "false"]]
        AdminConfig.create("Property", jvm, attrs)
    AdminConfig.save()


def main():
    action = sys.argv[0] if len(sys.argv) > 0 else "list"
    server_name = sys.argv[1] if len(sys.argv) > 1 else ""
    node_name = sys.argv[2] if len(sys.argv) > 2 else ""
    json_path = sys.argv[3] if len(sys.argv) > 3 else None

    server = resolve_server(server_name, node_name)
    if not server:
        print("RSP_ERROR: Server not found")
        sys.exit(2)

    jvm = resolve_jvm(server)
    if not jvm:
        print("RSP_ERROR: JavaVirtualMachine not found")
        sys.exit(2)

    if action == "list":
        emit_json(list_props(jvm))
        return

    if action == "apply":
        entries = []
        if json_path:
            with open(json_path, "r") as handle:
                entries = json.load(handle)
        apply_props(jvm, entries if isinstance(entries, list) else [])
        emit_json({"ok": True})
        return

    print("RSP_ERROR: Unknown action " + action)
    sys.exit(2)


main()
