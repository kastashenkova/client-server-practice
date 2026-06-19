## 📌 Practice 1. Message Encryption and Decryption

<img width="1713" height="729" alt="image" src="https://github.com/user-attachments/assets/b21c0c36-cdf3-4bb9-8486-4b715b55ff92" />

### Test coverage
<img width="1079" height="186" alt="image" src="https://github.com/user-attachments/assets/4b1fc824-2a60-497f-b883-1ab009a7b078" />

> made with [Jacoco](https://www.eclemma.org/jacoco/)

### Configuration
1. Clone repository
2. Copy `.env_sample` file into your `.env` and configure it in order to set up environment variable for secure message encryption.
   > Mind: the length of your `MESSAGE_SECRET_KEY` must be 16, 32 or 64 chars long
3. Test classes with special system tests

## 📌 Practice 2. Multithreading
<img width="701" height="208" alt="image" src="https://github.com/user-attachments/assets/5ce96ab5-0c40-46e0-b9d8-0e136151a3b7" />

### Test coverage
<img width="514" height="186" alt="image" src="https://github.com/user-attachments/assets/30778592-719d-44ab-b273-ae4bd837c853" />

> made with [Jacoco](https://www.eclemma.org/jacoco/)

## 📌 Practice 3. Networking
Added classes `StoreServerTCP`, `StoreClientTCP`, `StoreServerUDP` and `StoreClientUDP`

## 📌 Practice 4. Database
Product operations implementation:
- create
- getAll (with filters and pagination)
- getById
- update
- deleteAll
- deleteById
- count
  
### Test coverage
<img width="431" height="84" alt="image" src="https://github.com/user-attachments/assets/7a49d458-28a8-4be5-a725-2ad5948a04a6" />

> made with [Jacoco](https://www.eclemma.org/jacoco/)

## 📌 Practice 5. HTTP Server
Added endpoints:
- `POST /login`
- `GET /products/{id}`
- `PUT /products`
- `POST /products/{id}`
- `DELETE /products/{id}`

### Test coverage

> made with [Jacoco](https://www.eclemma.org/jacoco/)


> made with [Snyk](https://app.snyk.io/)
