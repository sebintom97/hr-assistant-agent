from dotenv import load_dotenv
import anthropic

load_dotenv()
client = anthropic.Anthropic()

# Call 1: the first question
response = client.messages.create(
    model="claude-haiku-5-5",
    max_tokens=1024,
    system="You are an HR assistant for Acme Analytics. Answer briefly.",
    messages=[
        {
            "role": "user",
            "content": "Check my available leaves",
        },
    ],
)
print("--- Call 1 ---")
for block in response.content:
    if block.type == "text":
        first_response = block.text  # save Claude's answer: it goes back into call 3's history
        print(block.text)

# Call 2: NO history, only the follow-up question
response2 = client.messages.create(
    model="claude-haiku-5-5",
    max_tokens=1024,
    system="You are an HR assistant for Acme Analytics. Answer briefly.",
    messages=[
        {
            "role": "user",
            "content": "What did I just ask you about?",
        },
    ],
)
print("--- Call 2 (no history) ---")
for block in response2.content:
    if block.type == "text":
        print(block.text)

# Call 3: FULL history, the real conversation in order
response3 = client.messages.create(
    model="claude-haiku-5-5",
    max_tokens=1024,
    system="You are an HR assistant for Acme Analytics. Answer briefly.",
    messages=[
        {
            "role": "user",
            "content": "Check my available leaves",
        },
        {
            "role": "assistant",
            "content": first_response,  # Claude's real answer to the question above (from call 1)
        },
        {
            "role": "user",
            "content": "What did I just ask you about?",
        },
    ],
)
print("--- Call 3 (full history) ---")
for block in response3.content:
    if block.type == "text":
        print(block.text)

print("stop_reason:", response3.stop_reason)
print("token in/out", response3.usage.input_tokens, response3.usage.output_tokens)
