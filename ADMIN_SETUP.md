# Initial administrator setup

Administrator accounts cannot be created through public candidate or recruiter registration. The first administrator is created with a one-time setup key.

1. Copy `.env.example` to `.env` and set `ADMIN_SETUP_KEY` to a long, random value. Keep this value private.
2. Start the services with Docker Compose so the auth service receives the key.
3. Open the frontend, select **Log in** or **Get started**, then choose **Set up the first administrator**.
4. Enter the administrator's name, email, password, and the setup key. The setup endpoint stops creating admins once an ADMIN account exists.
5. Remove `ADMIN_SETUP_KEY` from the environment and restart the auth service after setup.

Only ADMIN accounts can use the Security insights page. The API gateway and SOC service also validate the signed administrator token; hiding the navigation link is not the access control.
