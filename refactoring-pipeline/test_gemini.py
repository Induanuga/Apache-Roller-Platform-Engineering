import google.generativeai as genai
import os

# Read API key from environment variable
API_KEY = os.getenv("GEMINI_API_KEY")

if not API_KEY:
    print("ERROR: GEMINI_API_KEY environment variable not set")
    print("Run: export GEMINI_API_KEY=your_api_key_here")
    exit(1)

# Configure Gemini
genai.configure(api_key=API_KEY)

try:
    print("Connecting to Gemini API...")

    # Use a free-tier supported model
    model = genai.GenerativeModel("models/gemini-2.0-flash")

    response = model.generate_content("Hello Gemini! Are you working?")

    print("\n=== Gemini Response ===")
    print(response.text)

except Exception as e:
    print("Error while calling Gemini API:")
    print(e)
# import google.generativeai as genai
# import os

# API_KEY = os.getenv("GEMINI_API_KEY")

# if not API_KEY:
#     print("ERROR: GEMINI_API_KEY not set")
#     exit(1)

# genai.configure(api_key=API_KEY)

# print("Fetching available Gemini models...\n")

# try:
#     for m in genai.list_models():
#         print(m.name)
# except Exception as e:
#     print("Error listing models:", e)
