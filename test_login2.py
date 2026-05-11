import requests
import uuid

# create a new random user then log in and star.
s = requests.Session()
r = s.get("http://localhost:8080/roller-ui/login.rol")
data = {
    "j_username": "admin",
    "j_password": "password"
}
r2 = s.post("http://localhost:8080/roller_j_security_check", data=data)

r3 = s.get("http://localhost:8080/roller-ui/star!entry.rol?id=615cf5b1-fc25-4845-b7ba-f0e6c1f67c12", allow_redirects=False)
print("Star Action response:", r3.status_code)

r4 = s.get("http://localhost:8080/Weblog1A/")
if "Star this Entry [2]" in r4.text or "Star this Entry [3]" in r4.text:
    print("SUCCESS")
else:
    print("FAILED")
    print(r4.text)
