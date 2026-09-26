
We are going to implement this below services as a part of user services 
in this project.

1. Implementing Signup
2. Implementing Login
3. Implementing Logout
4. Implementing Validate

In this project we will be creating a Auth controller which will take care of all 
of the Authentication related things will be in Auth Controller.

List of Services to be created in the auth controller
1. Login
2. Logout
3. SignUp
4. Validate

We are going to create a Role Controler in which we will creating a new role of the 
user.


## Configuration

The service reads these environment variables:

- `USERSERVICE_DB_URL` and `USERSERVICE_DB_USERNAME`: the MySQL database.
- `USERSERVICE_JWT_SECRET`: a Base64-encoded key of at least 256 bits that signs login tokens. Generate one with `openssl rand -base64 32`. The service won't start with a shorter key.

Login tokens expire after 24 hours. `POST /auth/Logout` with `{"userId": ..., "token": "..."}` ends the session early.
