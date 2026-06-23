import urllib.request
import json

url = "http://localhost:8080/api/v1/dashboard/rings/top-threat"
try:
    response = urllib.request.urlopen(url)
    print("Success:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print(f"HTTP Error {e.code}:")
    print(e.read().decode('utf-8'))
except Exception as e:
    print("Other error:", str(e))
