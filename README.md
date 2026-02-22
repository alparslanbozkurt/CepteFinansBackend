# Cepte Finans Backend API

Cepte Finans, kullanıcıların gelirlerini, giderlerini ve bütçelerini güvenli ve akıllı bir şekilde yönetmelerini sağlayan yeni nesil bir kişisel finans uygulamasıdır. Bu depo, projenin **Security-First (Güvenlik Odaklı)** ve yüksek performanslı Spring Boot backend mimarisini içermektedir.

## Kullanılan Teknolojiler

- **Dil:** Java 21
- **Framework:** Spring Boot 3.x
- **Veritabanı:** PostgreSQL
- **ORM:** Hibernate / Spring Data JPA
- **Güvenlik:** Spring Security, JWT (JSON Web Tokens), BCrypt Şifreleme
- **Bağımlılık Yönetimi:** Maven
- **Yardımcı Araçlar:** Lombok, JJWT

---

## Modüller ve Özellikler (Tamamlananlar)

Uygulama, "Domain-Driven Design" (Etki Alanı Odaklı Tasarım) prensiplerine yakın bir paket yapısıyla modüllere ayrılmıştır:

### 1. Kimlik ve Güvenlik Modülü (Auth Domain)
- **JWT Altyapısı:** Stateless (durumsuz) kimlik doğrulama.
- **Çift Jeton Sistemi (Dual Token):**
  - 15 dakikalık kısa ömürlü **Access Token** (Yüksek güvenlik için).
  - 7 günlük uzun ömürlü **Refresh Token** (Kesintisiz kullanıcı deneyimi için).
- **Güvenli Çıkış (Logout):** Refresh token'ların veritabanından kalıcı olarak silinmesi (Revocation).
- **Veri Şifreleme:** Şifrelerin BCrypt algoritması ile hashlenerek saklanması.
- `fullName` desteği ile kişiselleştirilmiş profil altyapısı.

### 2. Finans Çekirdeği (Transaction Domain)
- **Kategori Sistemi (Data Seeding):** Uygulama ilk açıldığında varsayılan kategorilerin (Maaş, Market, Kira vb.) otomatik oluşturulması.
- **CRUD İşlemleri:** Harcama/Gelir ekleme, silme, güncelleme ve listeleme.
- **Akıllı Validasyonlar:** Bir "Gelir" kategorisine (Örn: Maaş) "Gider" işlemi girilmesinin backend seviyesinde engellenmesi.
- **Filtreleme:** İki tarih (startDate, endDate) aralığına göre harcama geçmişini filtreleme.

### 3. Bütçe Yönetimi (Budget Domain)
- **Dinamik Bütçe Periyotları:** Aylık, Haftalık ve Yıllık bütçe hedefleri belirleyebilme.
- **Canlı Hesaplama:** İlgili kategorideki harcamaların (Transactions) anlık olarak toplanıp bütçe doluluk oranının (Yüzde %) backend'de hesaplanması.
- **Çakışma Kontrolü (Overlap Validation):** Aynı ay ve aynı kategori için mükerrer bütçe oluşturulmasının engellenmesi.

---

## API Uç Noktaları (Endpoints)

> **Base URL:** `{{CF-localbaseURL}}/api/v1` veya `http://localhost:8080/api/v1`
>
> **Yetki gerektiren tüm isteklerde** Header'a şu eklenmeli: `Authorization: Bearer <Access_Token>`

---

### 🔑 Authentication (`/auth`)

#### `POST /auth/register` — Yeni Kullanıcı Kaydı
Yeni bir kullanıcı hesabı oluşturur.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `fullName` | string | Kullanıcının tam adı |
| `email` | string | E-posta adresi |
| `password` | string | Şifre |

```
POST {{CF-localbaseURL}}/api/v1/auth/register
Content-Type: application/json
```
```json
{
  "fullName": "Alparslan Bozkurt",
  "email": "deneme1@arvenlabs.com",
  "password": "GucluBirSifre123"
}
```


**Response:**
```json
{
  "success": true,
  "message": "Kayıt başarılı",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJmdWxsTmFtZSI6IkFscGFyc2xhbiBCb3prdXJ0Iiwic3ViIjoiZGVuZW1lM0BhcnZlbmxhYnMuY29tIiwiaWF0IjoxNzcxNzc2ODk2LCJleHAiOjE3NzE3Nzc3OTZ9.fguaddfPHtzm3k7moVir2sh9VfbHL_5WkDeMsY8KwU4",
    "refreshToken": "7b5625ed-e143-416c-9c5e-e29c0274029b",
    "message": "Kayıt işlemi başarılı."
  },
  "timestamp": "2026-02-22T19:14:56.537873"
}
```

---

#### `POST /auth/login` — Giriş Yap
Kullanıcı girişi yapar; Access Token ve Refresh Token döner.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `email` | string | Kayıtlı e-posta adresi |
| `password` | string | Şifre |

```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json
```
```json
{
  "email": "deneme@arvenlabs.com",
  "password": "GucluBirSifre123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Giriş başarılı",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJmdWxsTmFtZSI6IktpbmcgU2xheWVyIiwic3ViIjoiZGVuZW1lQGFydmVubGFicy5jb20iLCJpYXQiOjE3NzE3NzQ5NzgsImV4cCI6MTc3MTc3NTg3OH0.TbOuvD1OA_vz72BUr6NwS_IhmerM-KmPHXYq6T8WKDk",
    "refreshToken": "96c8f785-1fdf-4d29-8351-5b483ac19238",
    "message": "Giriş başarılı."
  },
  "timestamp": "2026-02-22T18:42:58.663258552"
}
```

---

#### `POST /auth/refresh-token` — Token Yenile
Süresi dolan Access Token'ı, geçerli Refresh Token ile yeniler. Yetki gerektirmez.

---

#### `POST /auth/logout` — Güvenli Çıkış ✅
Refresh Token'ı veritabanından kalıcı olarak siler. **Yetki gerektirir.**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `token` | string (UUID) | Geçerli Refresh Token değeri |

```
POST {{CF-localbaseURL}}/api/v1/auth/logout
Authorization: Bearer <Access_Token>
Content-Type: application/json
```
```json
{
  "token": "44fdc3c5-52b4-4c32-8699-9be21732bff0"
}
```

---

### 💳 Transactions (`/transactions`)

#### `POST /transactions` — Yeni İşlem Ekle ✅
Yeni bir gelir veya gider kaydı oluşturur.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `categoryId` | integer | Kategori ID'si |
| `type` | string | `INCOME` veya `EXPENSE` |
| `amount` | decimal | İşlem tutarı |
| `description` | string | Açıklama |
| `transactionDate` | string (YYYY-MM-DD) | İşlem tarihi |

```
POST http://localhost:8080/api/v1/transactions
Authorization: Bearer <Access_Token>
Content-Type: application/json
```
```json
{
  "categoryId": 6,
  "type": "EXPENSE",
  "amount": 2100.50,
  "description": "Haftalık market alışverişi",
  "transactionDate": "2026-02-09"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Kayıt Başarılı",
  "data": {
    "amount": 2100.50,
    "categoryIcon": "default-icon",
    "categoryName": "Market",
    "description": "Haftalık market alışverişi",
    "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
    "transactionDate": "2026-02-09",
    "type": "EXPENSE"
  },
  "timestamp": "2026-02-22T18:44:03.071128704"
}
```

---

#### `GET /transactions?startDate=&endDate=` — İşlemleri Listele ✅
Tüm işlemleri listeler. İsteğe bağlı tarih filtresi uygulanabilir.

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `startDate` | string (YYYY-MM-DD) | Hayır | Başlangıç tarihi |
| `endDate` | string (YYYY-MM-DD) | Hayır | Bitiş tarihi |

```
GET http://localhost:8080/api/v1/transactions?startDate=2026-02-01&endDate=2026-02-28
Authorization: Bearer <Access_Token>
```

**Response:**
```json
{
  "success": true,
  "message": "Filtrelenmiş işlemler",
  "data": [
    {
      "amount": 2100.50,
      "categoryIcon": "default-icon",
      "categoryName": "Market",
      "description": "Haftalık market alışverişi",
      "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
      "transactionDate": "2026-02-09",
      "type": "EXPENSE"
    }
  ],
  "timestamp": "2026-02-22T18:45:06.11827244"
}
```

---

#### `PUT /transactions/{id}` — İşlemi Güncelle ✅
Mevcut bir işlemi günceller. `{id}` alanına işlemin UUID değeri yazılır.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `categoryId` | integer | Kategori ID'si |
| `type` | string | `INCOME` veya `EXPENSE` |
| `amount` | decimal | Güncellenmiş tutar |
| `description` | string | Güncellenmiş açıklama |
| `transactionDate` | string (YYYY-MM-DD) | Güncellenmiş tarih |

```
PUT http://localhost:8080/api/v1/transactions/7a045cbf-8e32-4ae5-95a3-9fb2adf63758
Authorization: Bearer <Access_Token>
Content-Type: application/json
```
```json
{
  "categoryId": 6,
  "type": "EXPENSE",
  "amount": 99999.90,
  "description": "GÜNCELLENDİ: Bu harcama düzenlendi!",
  "transactionDate": "2026-02-10"
}
```

---

#### `DELETE /transactions/{id}` — İşlemi Sil ✅
Belirtilen ID'ye sahip işlemi siler.

```
DELETE http://localhost:8080/api/v1/transactions/{id}
Authorization: Bearer <Access_Token>
```

---

### 📈 Budgets (`/budgets`)

#### `POST /budgets` — Yeni Bütçe Oluştur ✅
Belirli bir kategori için bütçe hedefi tanımlar.

| Alan | Tip | Açıklama |
|------|-----|----------|
| `categoryId` | integer | Kategori ID'si |
| `amount` | decimal | Bütçe miktarı |
| `period` | string | `MONTHLY`, `WEEKLY` veya `YEARLY` |

```
POST http://localhost:8080/api/v1/budgets
Authorization: Bearer <Access_Token>
Content-Type: application/json
```
```json
{
  "categoryId": 6,
  "amount": 2000,
  "period": "MONTHLY"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Bütçe oluşturuldu",
  "data": {
    "categoryName": "Market",
    "endDate": "2026-02-28",
    "id": "da022e27-6cf7-454c-a052-53964e1c7619",
    "limitAmount": 2000,
    "percentage": 105.0,
    "period": "MONTHLY",
    "spentAmount": 2100.50,
    "startDate": "2026-02-01"
  },
  "timestamp": "2026-02-22T18:46:10.102391428"
}
```

---

#### `GET /budgets` — Bütçeleri Listele ✅
Tüm bütçeleri, harcanan tutarları ve doluluk yüzdelerini döner.

```
GET http://localhost:8080/api/v1/budgets
Authorization: Bearer <Access_Token>
```

---

#### `DELETE /budgets/{id}` — Bütçeyi Sil ✅
Belirtilen ID'ye sahip bütçeyi siler.

```
DELETE http://localhost:8080/api/v1/budgets/31260bd9-27d5-4d07-b85b-230d07258895
Authorization: Bearer <Access_Token>
```

---

## ⚙️ Kurulum ve Çalıştırma

1. Projeyi bilgisayarınıza indirin.
2. PostgreSQL veritabanınızda `ceptefinans_db` adında boş bir veritabanı oluşturun.
3. `src/main/resources/application.properties` dosyasındaki veritabanı kullanıcı adı ve şifrenizi kendi yerel ortamınıza göre düzenleyin. (Bağlantı portu: 5433).
4. Projeyi IDE'niz (IntelliJ IDEA vb.) üzerinden çalıştırın.
5. Veritabanı tabloları Hibernate (ddl-auto) tarafından otomatik oluşturulacak ve varsayılan kategoriler yüklenecektir.

---

*Gelecek Sürümlerde Planlananlar: Yapay Zeka (AI) destekli fiş okuma ve OCR entegrasyonu.*