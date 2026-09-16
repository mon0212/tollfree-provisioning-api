import json
import requests
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

API_BASE = "http://localhost:8080/api/v1"
OLLAMA_BASE = "http://localhost:11434"
MODEL = "llama3.2"
JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtZSIsInJvbGUiOiJBRE1JTiJ9.p-FLXAYDQO7605I2Ep1AF5TomtJBDSfyX8twGUTdfsk"

HEADERS = {"Authorization": f"Bearer {JWT}", "Content-Type": "application/json"}

# ── Tool definitions (sent to Ollama) ────────────────────────────────────────

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "create_customer",
            "description": "Create a new telecom customer with a name and email address.",
            "parameters": {
                "type": "object",
                "properties": {
                    "name": {"type": "string", "description": "Full name of the customer"},
                    "email": {"type": "string", "description": "Email address of the customer"},
                },
                "required": ["name", "email"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_customer",
            "description": "Look up a customer by their ID.",
            "parameters": {
                "type": "object",
                "properties": {
                    "customer_id": {"type": "string", "description": "UUID of the customer"},
                },
                "required": ["customer_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_customers",
            "description": "List all customers in the system.",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "import_number",
            "description": "Import a new toll-free number into the system. Number must be 10 digits starting with 800.",
            "parameters": {
                "type": "object",
                "properties": {
                    "number": {"type": "string", "description": "10-digit toll-free number starting with 800, e.g. 8001234567"},
                },
                "required": ["number"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_number",
            "description": "Get the current status and details of a toll-free number.",
            "parameters": {
                "type": "object",
                "properties": {
                    "number": {"type": "string", "description": "The toll-free number to look up"},
                },
                "required": ["number"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "provision_number",
            "description": "Reserve (provision) an available toll-free number for a specific customer. This is the correct way to assign a number — do not use set_number_status for the initial reservation.",
            "parameters": {
                "type": "object",
                "properties": {
                    "customer_id": {"type": "string", "description": "UUID of the customer"},
                    "number": {"type": "string", "description": "The toll-free number to provision"},
                },
                "required": ["customer_id", "number"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "set_number_status",
            "description": "Change the lifecycle status of a toll-free number. Valid transitions: RESERVED→ACTIVE, RESERVED→AVAILABLE, ACTIVE→SUSPENDED, SUSPENDED→ACTIVE, ACTIVE→RELEASED.",
            "parameters": {
                "type": "object",
                "properties": {
                    "number": {"type": "string", "description": "The toll-free number"},
                    "status": {
                        "type": "string",
                        "enum": ["AVAILABLE", "ACTIVE", "SUSPENDED", "RELEASED"],
                        "description": "Target status",
                    },
                },
                "required": ["number", "status"],
            },
        },
    },
]

# ── Tool execution ────────────────────────────────────────────────────────────

def execute_tool(name, args):
    try:
        if name == "create_customer":
            r = requests.post(f"{API_BASE}/customers", json=args, headers=HEADERS)
            return r.json()

        elif name == "get_customer":
            r = requests.get(f"{API_BASE}/customers/{args['customer_id']}", headers=HEADERS)
            return r.json()

        elif name == "list_customers":
            r = requests.get(f"{API_BASE}/customers", headers=HEADERS)
            data = r.json()
            return data.get("content", data)

        elif name == "import_number":
            r = requests.post(f"{API_BASE}/numbers", json={"number": args["number"]}, headers=HEADERS)
            return r.json()

        elif name == "get_number":
            r = requests.get(f"{API_BASE}/numbers/{args['number']}", headers=HEADERS)
            return r.json()

        elif name == "provision_number":
            payload = {"customerId": args["customer_id"], "number": args["number"]}
            r = requests.post(f"{API_BASE}/provisioning", json=payload, headers=HEADERS)
            return r.json()

        elif name == "set_number_status":
            r = requests.post(
                f"{API_BASE}/numbers/{args['number']}/actions/{args['status']}",
                headers=HEADERS,
            )
            return r.json()

        else:
            return {"error": f"Unknown tool: {name}"}

    except Exception as e:
        return {"error": str(e)}


# ── Agent loop ────────────────────────────────────────────────────────────────

def run_agent(user_message, history):
    system_prompt = (
        "You are a helpful assistant for managing telecom toll-free numbers. "
        "You have tools to create customers, import numbers, provision (reserve) numbers for customers, "
        "change number lifecycle status, and look up information. "
        "Always use the tools to perform actions — never make up data. "
        "When a user asks to provision or reserve a number for a customer, use the provision_number tool. "
        "After each tool call, summarize what happened in plain English."
    )

    messages = [{"role": "system", "content": system_prompt}]
    for h in history:
        messages.append(h)
    messages.append({"role": "user", "content": user_message})

    tool_calls_made = []

    # Agentic loop — keep calling Ollama until it stops requesting tools
    for _ in range(5):
        resp = requests.post(
            f"{OLLAMA_BASE}/api/chat",
            json={"model": MODEL, "messages": messages, "tools": TOOLS, "stream": False},
        )
        resp.raise_for_status()
        reply = resp.json()["message"]
        messages.append(reply)

        if not reply.get("tool_calls"):
            break

        # Execute each tool call
        for tc in reply["tool_calls"]:
            fn = tc["function"]
            tool_name = fn["name"]
            tool_args = fn.get("arguments", {})
            if isinstance(tool_args, str):
                tool_args = json.loads(tool_args)

            result = execute_tool(tool_name, tool_args)
            tool_calls_made.append({"tool": tool_name, "args": tool_args, "result": result})

            messages.append({
                "role": "tool",
                "content": json.dumps(result),
            })

    final_content = messages[-1].get("content") or "Done."
    return final_content, tool_calls_made, messages[1:]  # strip system from history


# ── Flask endpoint ────────────────────────────────────────────────────────────

@app.route("/chat", methods=["POST"])
def chat():
    body = request.get_json()
    user_message = body.get("message", "")
    history = body.get("history", [])

    reply, tool_calls, updated_history = run_agent(user_message, history)

    return jsonify({
        "reply": reply,
        "tool_calls": tool_calls,
        "history": updated_history,
    })


@app.route("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(port=5001, debug=False)
