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
- **E-posta Doğrulama:** Kayıt sonrası e-posta doğrulama kodu gönderimi ve doğrulama.
- **Güvenli Çıkış (Logout):** Refresh token'ların veritabanından kalıcı olarak silinmesi (Revocation).
- **Veri Şifreleme:** Şifrelerin BCrypt algoritması ile hashlenerek saklanması.
- `fullName` desteği ile kişiselleştirilmiş profil altyapısı.

### 2. Finans Çekirdeği (Transaction Domain)
- **Kategori Sistemi (Data Seeding):** Uygulama ilk açıldığında varsayılan kategorilerin (Maaş, Market, Kira vb.) otomatik oluşturulması.
- **CRUD İşlemleri:** Harcama/Gelir ekleme, silme, güncelleme ve listeleme.
- **Akıllı Validasyonlar:** Bir "Gelir" kategorisine (Örn: Maaş) "Gider" işlemi girilmesinin backend seviyesinde engellenmesi.
- **Filtreleme:** İki tarih (startDate, endDate) aralığına göre harcama geçmişini filtreleme.
- **Kaynak Takibi:** İşlemlerin kaynağını takip etme (`MANUAL`, `SMS`, `OCR`).

### 3. Bütçe Yönetimi (Budget Domain)
- **Dinamik Bütçe Periyotları:** Aylık, Haftalık ve Yıllık bütçe hedefleri belirleyebilme.
- **Canlı Hesaplama:** İlgili kategorideki harcamaların (Transactions) anlık olarak toplanıp bütçe doluluk oranının (Yüzde %) backend'de hesaplanması.
- **Çakışma Kontrolü (Overlap Validation):** Aynı ay ve aynı kategori için mükerrer bütçe oluşturulmasının engellenmesi.

### 4. Bildirim Modülü (Notification Domain)
- **Bildirim Listeleme:** Kullanıcıya ait tüm bildirimlerin listelenmesi.
- **Okunmamış Sayısı:** Okunmamış bildirim sayısını getirme.
- **Okundu İşaretleme:** Bildirimleri okundu olarak işaretleme.

### 5. Altyapı Test Modülü (Infrastructure Test)
- **Sağlık Kontrolü (Ping):** Sistemin ayakta olup olmadığını kontrol eden endpoint.
- **Hata Testi:** GlobalExceptionHandler'ın doğru çalıştığını test eden endpoint.

---

## Genel API Yanıt Yapısı (ApiResponse)

Tüm endpointler aşağıdaki genel sarmalayıcı (wrapper) yapı ile yanıt döner:

```json
{
  "success": true,
  "message": "İşlem mesajı",
  "data": {  },
  "timestamp": "2026-02-22T19:14:56.537873"
}
```

| Alan | Tip | Açıklama |
|------|-----|----------|
| `success` | boolean | İşlem başarılı mı? (`true` / `false`) |
| `message` | string | İşlem hakkında açıklama mesajı |
| `data` | object / array / null | Dönen veri (başarılı ise ilgili nesne, hata ise `null`) |
| `timestamp` | string (ISO 8601) | Yanıtın oluşturulma zamanı |

### Hata Yanıtı (Error Response)

```json
{
  "success": false,
  "message": "Sunucu tarafında bir hata oluştu: Hata detayı",
  "data": null,
  "timestamp": "2026-02-22T19:14:56.537873"
}
```

### Validation Hata Yanıtı

```json
{
  "success": true,
  "message": "Veri doğrulama hatası",
  "data": {
    "email": "Geçerli bir e-posta adresi giriniz",
    "password": "Şifre boş olamaz"
  },
  "timestamp": "2026-02-22T19:14:56.537873"
}
```

---

## Enum Değerleri

### CategoryType
| Değer | Açıklama |
|-------|----------|
| `INCOME` | Gelir |
| `EXPENSE` | Gider |

### BudgetPeriod
| Değer | Açıklama |
|-------|----------|
| `WEEKLY` | Haftalık |
| `MONTHLY` | Aylık |
| `YEARLY` | Yıllık |

### TransactionSource
| Değer | Açıklama |
|-------|----------|
| `MANUAL` | Elle girilen |
| `SMS` | Banka SMS'inden gelen |
| `OCR` | Fiş tarama ile gelen |

---

## API Uç Noktaları (Endpoints)

> **Base URL:** `https://api.ceptefinans.arvenlabs.com/api/v1`
>
> **Yetki gerektiren tüm isteklerde** Header'a şu eklenmeli: `Authorization: Bearer <Access_Token>`

---

### 🔑 Authentication (`/api/v1/auth`)

---

#### 1. `POST /api/v1/auth/register` — Yeni Kullanıcı Kaydı

Yeni bir kullanıcı hesabı oluşturur. Kayıt başarılı olduğunda **token dönmez**; bunun yerine kullanıcının e-posta adresine 6 haneli bir doğrulama kodu gönderilir. Token almak için `/auth/verify-email` endpoint'i kullanılmalıdır.

> **Not:** Doğrulama kodu 5 dakika geçerlidir.

**Yetki:** Gerekmiyor

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `fullName` | string | Evet | Kullanıcının tam adı |
| `email` | string | Evet | E-posta adresi |
| `password` | string | Evet | Şifre |

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Alparslan Bozkurt",
  "email": "deneme1@arvenlabs.com",
  "password": "GucluBirSifre123"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Kayıt başarılı",
  "data": {
    "accessToken": null,
    "refreshToken": null,
    "message": "Kayıt başarılı. Lütfen e-postanıza gönderilen 6 haneli kodu girin."
  },
  "timestamp": "2026-02-22T19:14:56.537873"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `accessToken` | null | Bu aşamada token **verilmez** |
| `refreshToken` | null | Bu aşamada token **verilmez** |
| `message` | string | Kullanıcıyı e-posta doğrulamasına yönlendiren mesaj |

---

#### 2. `POST /api/v1/auth/login` — Giriş Yap

Kullanıcı girişi yapar; Access Token ve Refresh Token döner.

> **Önemli:** E-postası doğrulanmamış kullanıcılar giriş yapamaz. Bu durumda `"Lütfen giriş yapmadan önce e-posta adresinizi onaylayın."` hatası döner.

**Yetki:** Gerekmiyor

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `email` | string | Evet | Kayıtlı e-posta adresi |
| `password` | string | Evet | Şifre |

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "deneme@arvenlabs.com",
  "password": "GucluBirSifre123"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Giriş başarılı",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJmdWxsTmFtZSI6Iktpbmcg...",
    "refreshToken": "96c8f785-1fdf-4d29-8351-5b483ac19238",
    "message": "Giriş başarılı."
  },
  "timestamp": "2026-02-22T18:42:58.663258552"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `accessToken` | string (JWT) | Kimlik doğrulama için kullanılan kısa ömürlü token (15 dk) |
| `refreshToken` | string (UUID) | Access token yenilemek için kullanılan uzun ömürlü token (7 gün) |
| `message` | string | İşlem sonuç mesajı |

---

#### 3. `POST /api/v1/auth/refresh-token` — Token Yenile

Süresi dolan Access Token'ı, geçerli Refresh Token ile yeniler.

**Yetki:** Gerekmiyor

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `token` | string (UUID) | Evet | Geçerli Refresh Token değeri |

```http
POST /api/v1/auth/refresh-token
Content-Type: application/json
```

```json
{
  "token": "96c8f785-1fdf-4d29-8351-5b483ac19238"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Token yenilendi",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.yeniAccessToken...",
    "refreshToken": "yeni-refresh-token-uuid",
    "message": "Token yenilendi."
  },
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `accessToken` | string (JWT) | Yeni oluşturulan Access Token |
| `refreshToken` | string (UUID) | Yeni oluşturulan Refresh Token |
| `message` | string | İşlem sonuç mesajı |

---

#### 4. `POST /api/v1/auth/logout` — Güvenli Çıkış

Refresh Token'ı veritabanından kalıcı olarak siler (Revocation).

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `token` | string (UUID) | Evet | Silinecek Refresh Token değeri |

```http
POST /api/v1/auth/logout
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "token": "44fdc3c5-52b4-4c32-8699-9be21732bff0"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Çıkış başarılı",
  "data": null,
  "timestamp": "2026-02-22T19:10:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `data` | null | Çıkış işleminde veri dönmez |

---

#### 5. `POST /api/v1/auth/verify-email` — E-posta Doğrulama

Kayıt sonrası e-postaya gönderilen 6 haneli kodu doğrular. Başarılı doğrulama sonucunda kullanıcı sisteme kabul edilir ve **Access Token + Refresh Token** döner. Bu endpoint, `/auth/register` ile başlayan akışın tamamlandığı adımdır.

> **Hata Durumları:**
> - Kod hatalıysa: `"Girdiğiniz kod hatalı!"`
> - Kodun süresi dolduysa (5 dk): `"Bu kodun süresi dolmuş. Lütfen yeni bir kod isteyin."`
> - E-posta zaten doğrulandıysa: `"E-posta adresiniz zaten onaylanmış. Giriş yapabilirsiniz."`

**Yetki:** Gerekmiyor

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `email` | string | Evet | Doğrulanacak e-posta adresi |
| `code` | string | Evet | E-posta ile gönderilen 6 haneli doğrulama kodu |

```http
POST /api/v1/auth/verify-email
Content-Type: application/json
```

```json
{
  "email": "deneme1@arvenlabs.com",
  "code": "048312"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "E-posta doğrulandı! Hoş geldiniz.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.dogrulanmisToken...",
    "refreshToken": "yeni-refresh-token-uuid",
    "message": "E-posta doğrulandı! Hoş geldiniz."
  },
  "timestamp": "2026-02-22T19:15:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `accessToken` | string (JWT) | Doğrulama sonrası oluşturulan Access Token (15 dk) |
| `refreshToken` | string (UUID) | Doğrulama sonrası oluşturulan Refresh Token (7 gün) |
| `message` | string | İşlem sonuç mesajı |

---

### 💳 Transactions (`/api/v1/transactions`)

---

#### 6. `POST /api/v1/transactions` — Yeni İşlem Ekle

Yeni bir gelir veya gider kaydı oluşturur.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `categoryId` | integer (Long) | Evet | Kategori ID'si |
| `type` | string (enum) | Evet | `INCOME` veya `EXPENSE` |
| `amount` | decimal (BigDecimal) | Evet | İşlem tutarı |
| `description` | string | Evet | İşlem açıklaması |
| `transactionDate` | string (YYYY-MM-DD) | Evet | İşlem tarihi |
| `source` | string (enum) | Hayır | `MANUAL` (varsayılan), `SMS` veya `OCR` |

```http
POST /api/v1/transactions
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "categoryId": 6,
  "type": "EXPENSE",
  "amount": 2100.50,
  "description": "Haftalık market alışverişi",
  "transactionDate": "2026-02-09",
  "source": "MANUAL"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Kayıt Başarılı",
  "data": {
    "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
    "categoryName": "Market",
    "categoryIcon": "default-icon",
    "type": "EXPENSE",
    "amount": 2100.50,
    "description": "Haftalık market alışverişi",
    "transactionDate": "2026-02-09"
  },
  "timestamp": "2026-02-22T18:44:03.071128704"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | İşlemin benzersiz kimliği |
| `categoryName` | string | Kategorinin adı (Örn: "Market") |
| `categoryIcon` | string | Kategorinin ikonu |
| `type` | string (enum) | `INCOME` veya `EXPENSE` |
| `amount` | decimal | İşlem tutarı |
| `description` | string | İşlem açıklaması |
| `transactionDate` | string (YYYY-MM-DD) | İşlem tarihi |

---

#### 7. `GET /api/v1/transactions` — İşlemleri Listele

Tüm işlemleri listeler. İsteğe bağlı tarih filtresi uygulanabilir.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Query Parameters:**

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `startDate` | string (YYYY-MM-DD) | Hayır | Başlangıç tarihi |
| `endDate` | string (YYYY-MM-DD) | Hayır | Bitiş tarihi |

> **Not:** `startDate` ve `endDate` birlikte gönderilmelidir. İkisi de gönderilmezse tüm işlemler döner.

```http
GET /api/v1/transactions?startDate=2026-02-01&endDate=2026-02-28
Authorization: Bearer <Access_Token>
```

**Response (200 OK) — Filtreli:**

```json
{
  "success": true,
  "message": "Filtrelenmiş işlemler",
  "data": [
    {
      "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
      "categoryName": "Market",
      "categoryIcon": "default-icon",
      "type": "EXPENSE",
      "amount": 2100.50,
      "description": "Haftalık market alışverişi",
      "transactionDate": "2026-02-09"
    }
  ],
  "timestamp": "2026-02-22T18:45:06.11827244"
}
```

**Response (200 OK) — Filtresiz:**

```json
{
  "success": true,
  "message": "Tüm işlemler",
  "data": [
    {
      "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
      "categoryName": "Market",
      "categoryIcon": "default-icon",
      "type": "EXPENSE",
      "amount": 2100.50,
      "description": "Haftalık market alışverişi",
      "transactionDate": "2026-02-09"
    }
  ],
  "timestamp": "2026-02-22T18:45:06.11827244"
}
```

**Response `data` Dizisi İçindeki Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | İşlemin benzersiz kimliği |
| `categoryName` | string | Kategorinin adı |
| `categoryIcon` | string | Kategorinin ikonu |
| `type` | string (enum) | `INCOME` veya `EXPENSE` |
| `amount` | decimal | İşlem tutarı |
| `description` | string | İşlem açıklaması |
| `transactionDate` | string (YYYY-MM-DD) | İşlem tarihi |

---

#### 8. `PUT /api/v1/transactions/{id}` — İşlemi Güncelle

Mevcut bir işlemi günceller. `{id}` alanına işlemin UUID değeri yazılır.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Güncellenecek işlemin ID'si |

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `categoryId` | integer (Long) | Evet | Kategori ID'si |
| `type` | string (enum) | Evet | `INCOME` veya `EXPENSE` |
| `amount` | decimal (BigDecimal) | Evet | Güncellenmiş tutar |
| `description` | string | Evet | Güncellenmiş açıklama |
| `transactionDate` | string (YYYY-MM-DD) | Evet | Güncellenmiş tarih |
| `source` | string (enum) | Hayır | `MANUAL`, `SMS` veya `OCR` |

```http
PUT /api/v1/transactions/7a045cbf-8e32-4ae5-95a3-9fb2adf63758
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

**Response (200 OK):**

```json
{
  "success": true,
  "message": "İşlem güncellendi",
  "data": {
    "id": "7a045cbf-8e32-4ae5-95a3-9fb2adf63758",
    "categoryName": "Market",
    "categoryIcon": "default-icon",
    "type": "EXPENSE",
    "amount": 99999.90,
    "description": "GÜNCELLENDİ: Bu harcama düzenlendi!",
    "transactionDate": "2026-02-10"
  },
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | Güncellenen işlemin benzersiz kimliği |
| `categoryName` | string | Kategorinin adı |
| `categoryIcon` | string | Kategorinin ikonu |
| `type` | string (enum) | `INCOME` veya `EXPENSE` |
| `amount` | decimal | Güncellenmiş tutar |
| `description` | string | Güncellenmiş açıklama |
| `transactionDate` | string (YYYY-MM-DD) | Güncellenmiş tarih |

---

#### 9. `DELETE /api/v1/transactions/{id}` — İşlemi Sil

Belirtilen ID'ye sahip işlemi siler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Silinecek işlemin ID'si |

```http
DELETE /api/v1/transactions/eb85b6cc-7272-4c11-ba19-6f6829934c2c
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "İşlem silindi",
  "data": null,
  "timestamp": "2026-02-22T19:05:00.000000"
}
```

---

### 📈 Budgets (`/api/v1/budgets`)

---

#### 10. `POST /api/v1/budgets` — Yeni Bütçe Oluştur

Belirli bir kategori için bütçe hedefi tanımlar.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `categoryId` | integer (Long) | Evet | Kategori ID'si |
| `amount` | decimal (BigDecimal) | Evet | Bütçe limiti |
| `period` | string (enum) | Hayır | `MONTHLY` (varsayılan), `WEEKLY` veya `YEARLY` |

```http
POST /api/v1/budgets
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

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bütçe oluşturuldu",
  "data": {
    "id": "da022e27-6cf7-454c-a052-53964e1c7619",
    "categoryName": "Market",
    "limitAmount": 2000,
    "spentAmount": 2100.50,
    "percentage": 105.0,
    "period": "MONTHLY",
    "startDate": "2026-02-01",
    "endDate": "2026-02-28"
  },
  "timestamp": "2026-02-22T18:46:10.102391428"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | Bütçenin benzersiz kimliği |
| `categoryName` | string | Bütçenin ait olduğu kategorinin adı |
| `limitAmount` | decimal | Belirlenen bütçe limiti |
| `spentAmount` | decimal | Bu kategoride şu ana kadar yapılan harcama toplamı |
| `percentage` | double | Bütçe doluluk oranı (%) — Örn: `105.0` → %105 aşılmış |
| `period` | string (enum) | `WEEKLY`, `MONTHLY` veya `YEARLY` |
| `startDate` | string (YYYY-MM-DD) | Bütçe periyodunun başlangıç tarihi |
| `endDate` | string (YYYY-MM-DD) | Bütçe periyodunun bitiş tarihi |

---

#### 11. `GET /api/v1/budgets` — Bütçeleri Listele

Tüm bütçeleri, harcanan tutarları ve doluluk yüzdelerini döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

```http
GET /api/v1/budgets
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bütçeler listelendi",
  "data": [
    {
      "id": "da022e27-6cf7-454c-a052-53964e1c7619",
      "categoryName": "Market",
      "limitAmount": 2000,
      "spentAmount": 2100.50,
      "percentage": 105.0,
      "period": "MONTHLY",
      "startDate": "2026-02-01",
      "endDate": "2026-02-28"
    },
    {
      "id": "f1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "categoryName": "Ulaşım",
      "limitAmount": 500,
      "spentAmount": 250.00,
      "percentage": 50.0,
      "period": "MONTHLY",
      "startDate": "2026-02-01",
      "endDate": "2026-02-28"
    }
  ],
  "timestamp": "2026-02-22T18:50:00.000000"
}
```

**Response `data` Dizisi İçindeki Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | Bütçenin benzersiz kimliği |
| `categoryName` | string | Bütçenin ait olduğu kategorinin adı |
| `limitAmount` | decimal | Belirlenen bütçe limiti |
| `spentAmount` | decimal | Şu ana kadar yapılan harcama toplamı |
| `percentage` | double | Bütçe doluluk oranı (%) |
| `period` | string (enum) | `WEEKLY`, `MONTHLY` veya `YEARLY` |
| `startDate` | string (YYYY-MM-DD) | Bütçe periyodunun başlangıç tarihi |
| `endDate` | string (YYYY-MM-DD) | Bütçe periyodunun bitiş tarihi |

---

#### 12. `DELETE /api/v1/budgets/{id}` — Bütçeyi Sil

Belirtilen ID'ye sahip bütçeyi siler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Silinecek bütçenin ID'si |

```http
DELETE /api/v1/budgets/31260bd9-27d5-4d07-b85b-230d07258895
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bütçe silindi",
  "data": null,
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

---

### 🔔 Notifications (`/api/v1/notifications`)

---

#### 13. `GET /api/v1/notifications` — Bildirimleri Listele

Giriş yapan kullanıcıya ait tüm bildirimleri listeler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

```http
GET /api/v1/notifications
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bildirimler listelendi",
  "data": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "title": "Bütçe Uyarısı",
      "message": "Market kategorisinde bütçenizin %105'ini harcadınız!",
      "isRead": false,
      "createdAt": "2026-02-22T18:50:00.000000"
    },
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "title": "Hoş Geldiniz",
      "message": "Cepte Finans'a hoş geldiniz!",
      "isRead": true,
      "createdAt": "2026-02-20T10:00:00.000000"
    }
  ],
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

**Response `data` Dizisi İçindeki Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | string (UUID) | Bildirimin benzersiz kimliği |
| `title` | string | Bildirim başlığı |
| `message` | string | Bildirim içeriği |
| `isRead` | boolean | Bildirim okundu mu? (`true` / `false`) |
| `createdAt` | string (ISO 8601) | Bildirimin oluşturulma zamanı |

---

#### 14. `GET /api/v1/notifications/unread-count` — Okunmamış Bildirim Sayısı

Giriş yapan kullanıcının okunmamış bildirim sayısını döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

```http
GET /api/v1/notifications/unread-count
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Okunmamış bildirim sayısı",
  "data": 3,
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

**Response `data` Alanı:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `data` | integer (Long) | Okunmamış bildirim sayısı |

---

#### 15. `PUT /api/v1/notifications/{id}/read` — Bildirimi Okundu Olarak İşaretle

Belirtilen ID'ye sahip bildirimi okundu olarak işaretler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Okundu olarak işaretlenecek bildirimin ID'si |

```http
PUT /api/v1/notifications/a1b2c3d4-e5f6-7890-abcd-ef1234567890/read
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bildirim okundu olarak işaretlendi",
  "data": null,
  "timestamp": "2026-02-22T19:05:00.000000"
}
```

---

### 🔧 Infrastructure Test (`/api/v1/infra-test`)

---

#### 16. `GET /api/v1/infra-test/ping` — Sistem Sağlık Kontrolü

Sistemin ayakta olup olmadığını kontrol eder.

**Yetki:** Gerekmiyor

```http
GET /api/v1/infra-test/ping
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Altyapı sorunsuz çalışıyor.",
  "data": "Pong!",
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

**Response `data` Alanı:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `data` | string | Ping yanıtı: `"Pong!"` |

---

#### 17. `GET /api/v1/infra-test/error-test` — Hata Testi

GlobalExceptionHandler'ın doğru çalıştığını test etmek için kasıtlı hata fırlatır.

**Yetki:** Gerekmiyor

```http
GET /api/v1/infra-test/error-test
```

**Response (500 Internal Server Error):**

```json
{
  "success": false,
  "message": "Sunucu tarafında bir hata oluştu: Test amaçlı fırlatılan hata!",
  "data": null,
  "timestamp": "2026-02-22T19:00:00.000000"
}
```

---

## Endpoint Özet Tablosu

| # | Metod | Endpoint | Açıklama | Yetki |
|---|-------|----------|----------|-------|
| 1 | `POST` | `/api/v1/auth/register` | Yeni kullanıcı kaydı | ❌ |
| 2 | `POST` | `/api/v1/auth/login` | Giriş yap | ❌ |
| 3 | `POST` | `/api/v1/auth/refresh-token` | Token yenile | ❌ |
| 4 | `POST` | `/api/v1/auth/logout` | Güvenli çıkış | ✅ |
| 5 | `POST` | `/api/v1/auth/verify-email` | E-posta doğrulama | ❌ |
| 6 | `POST` | `/api/v1/transactions` | Yeni işlem ekle | ✅ |
| 7 | `GET` | `/api/v1/transactions` | İşlemleri listele/filtrele | ✅ |
| 8 | `PUT` | `/api/v1/transactions/{id}` | İşlemi güncelle | ✅ |
| 9 | `DELETE` | `/api/v1/transactions/{id}` | İşlemi sil | ✅ |
| 10 | `POST` | `/api/v1/budgets` | Yeni bütçe oluştur | ✅ |
| 11 | `GET` | `/api/v1/budgets` | Bütçeleri listele | ✅ |
| 12 | `DELETE` | `/api/v1/budgets/{id}` | Bütçeyi sil | ✅ |
| 13 | `GET` | `/api/v1/notifications` | Bildirimleri listele | ✅ |
| 14 | `GET` | `/api/v1/notifications/unread-count` | Okunmamış bildirim sayısı | ✅ |
| 15 | `PUT` | `/api/v1/notifications/{id}/read` | Bildirimi okundu işaretle | ✅ |
| 16 | `GET` | `/api/v1/infra-test/ping` | Sistem sağlık kontrolü | ❌ |
| 17 | `GET` | `/api/v1/infra-test/error-test` | Hata testi | ❌ |

---

## ⚙️ Kurulum ve Çalıştırma

1. Projeyi bilgisayarınıza indirin.
2. PostgreSQL veritabanınızda `ceptefinans_db` adında boş bir veritabanı oluşturun.
3. `src/main/resources/application.properties` dosyasındaki veritabanı kullanıcı adı ve şifrenizi kendi yerel ortamınıza göre düzenleyin. (Bağlantı portu: 5433).
4. Projeyi IDE'niz (IntelliJ IDEA vb.) üzerinden çalıştırın.
5. Veritabanı tabloları Hibernate (ddl-auto) tarafından otomatik oluşturulacak ve varsayılan kategoriler yüklenecektir.

---

*Gelecek Sürümlerde Planlananlar: Yapay Zeka (AI) destekli fiş okuma ve OCR entegrasyonu.*