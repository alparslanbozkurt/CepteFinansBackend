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
- **Yapay Zeka:** Google Gemini 2.5 Flash API (Vision)

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
- **Şifre Sıfırlama (Forgot Password):** E-posta yoluyla 15 dakika geçerli sıfırlama bağlantısı gönderimi. Geçersiz e-posta durumunda hata fırlatarak kullanıcıyı bilgilendirme.
- `fullName` desteği ile kişiselleştirilmiş profil altyapısı.

### 2. Finans Çekirdeği (Transaction Domain)
- **Kategori Sistemi (Data Seeding):** Uygulama ilk açıldığında varsayılan kategorilerin (Maaş, Market, Kira vb.) otomatik oluşturulması.
- **CRUD İşlemleri:** Harcama/Gelir ekleme, silme, güncelleme ve listeleme.
- **Akıllı Validasyonlar:** Bir "Gelir" kategorisine (Örn: Maaş) "Gider" işlemi girilmesinin backend seviyesinde engellenmesi.
- **Filtreleme:** İki tarih (startDate, endDate) aralığına göre harcama geçmişini filtreleme.
- **Kaynak Takibi:** İşlemlerin kaynağını takip etme (`MANUAL`, `SMS`, `OCR`).
- **AI OCR (Fiş Okuma) [YENİ]:** Google Gemini 2.5 Flash ile fiş fotoğraflarını analiz ederek tutar, tarih, kurum ve kategori bilgilerini otomatik çıkarma.
- **AI SMS Analizi [YENİ]:** Bankalardan gelen harcama/gelir SMS'lerini yapay zeka ile analiz ederek otomatik işlem verisi oluşturma.

### 3. Bütçe Yönetimi (Budget Domain)
- **Dinamik Bütçe Periyotları:** Aylık, Haftalık ve Yıllık bütçe hedefleri belirleyebilme.
- **Canlı Hesaplama:** İlgili kategorideki harcamaların (Transactions) anlık olarak toplanıp bütçe doluluk oranının (Yüzde %) backend'de hesaplanması.
- **Çakışma Kontrolü (Overlap Validation):** Aynı ay ve aynı kategori için mükerrer bütçe oluşturulmasının engellenmesi.

### 4. Bildirim Modülü (Notification Domain)
- **Bildirim Listeleme:** Kullanıcıya ait tüm bildirimlerin listelenmesi.
- **Okunmamış Sayısı:** Okunmamış bildirim sayısını getirme.
- **Okundu İşaretleme:** Bildirimleri okundu olarak işaretleme.

### 6. Pinti Özel Özellikleri (Pinti Domain) [YENİ]
- **Bento Layout:** Kullanıcı arayüz yerleşiminin (widget dizilimi) bulutta saklanması.
- **Wealth Orbit (Analitik):** Bütçe limitine göre harcama hızını gezegen hızı olarak simüle eden analitik motoru.
- **Impulse Vault (Dürtüsel Kasa):** 48 saatlik harcamama yemini (kilidi) ve kilit kırma (BROKEN status) kontrolü.
- **AI Roast (Gemini):** Haftalık harcamaların Gemini AI tarafından sarkastik bir dille eleştirilmesi.
- **Gmail Analizi:** Gmail üzerindeki e-faturaların otomatik taranması ve finansal verilere dönüştürülmesi.
- **Hedef ve Alışkanlık Takibi:** Finansal birikim hedefleri ve 30 günlük harcamasız gün (streak) heatmap verisi.
- **Push Bildirimleri (Firebase FCM) [YENİ]:** Kullanıcı cihazlarına mobil bildirim gönderimi, cihaz token yönetimi ve test bildirim altyapısı.

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
| `GMAIL` | Gmail e-posta tarama ile gelen |

### LockStatus (Impulse Vault)
| Değer | Açıklama |
|-------|----------|
| `ACTIVE` | Kilit aktif (48 saat dolmadı) |
| `BROKEN` | Kilit harcama yapılarak kırıldı |
| `UNLOCKED` | 48 saat başarıyla doldu ve kilit açıldı |

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

| `fullName` | string | Evet | Kullanıcının tam adı |
| `email` | string | Evet | E-posta adresi |
| `password` | string | Evet | Şifre |
| `isTermsAccepted` | boolean | Evet | Kullanım koşullarının kabulü (true olmalı) |

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "fullName": "Alparslan Bozkurt",
  "email": "deneme1@arvenlabs.com",
  "password": "GucluBirSifre123",
  "isTermsAccepted": true
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

#### 35. `POST /api/v1/users/fcm-token` — FCM Cihaz Token Kaydet

Kullanıcının giriş yaptığı cihazın Firebase Cloud Messaging (FCM) token'ını kaydeder.
Birden fazla cihaz (Web, Android, iOS) desteklenir. Gelen token boş/null ise mevcut mobil cihazların token'ı ezilmez (Web client login'lerinde koruma).

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `token` | string | Evet | Cihaza ait eşsiz FCM token'ı |
| `platform` | string | Hayır | Cihaz platformu (`ANDROID`, `IOS`, `WEB`) (Varsayılan: `UNKNOWN`) |

```http
POST /api/v1/users/fcm-token
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "token": "d7Hk_J1wQ...",
  "platform": "ANDROID"
}
```

**Response (200 OK):**
```json
"Cihaz token başarıyla kaydedildi. Platform: ANDROID"
```
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

#### 6. `POST /api/v1/auth/forgot-password` — Şifre Sıfırlama Bağlantısı Gönder

Kullanıcının e-posta adresine şifre sıfırlama bağlantısı gönderir. Girilen e-posta sistemde kayıtlı değilse hata döner.

> **Not:** Sıfırlama bağlantısı 15 dakika geçerlidir.

**Yetki:** Gerekmiyor

**Query Parameters:**

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `email` | string | Evet | Şifresi sıfırlanacak e-posta adresi |

```http
POST /api/v1/auth/forgot-password?email=deneme@arvenlabs.com
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.",
  "data": null,
  "timestamp": "2026-02-22T19:20:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `data` | null | Bu işlemde veri dönmez |

---

#### 7. `POST /api/v1/auth/reset-password` — Şifreyi Sıfırla

`/auth/forgot-password` adımında e-posta ile gönderilen token'ı kullanarak kullanıcının şifresini günceller. Token tek kullanımlıktır; başarılı işlem sonrasında silinir.

> **Hata Durumları:**
> - Token geçersiz veya bulunamıyorsa: `"Geçersiz veya bulunamayan token."`
> - Token süresi dolmuşsa (15 dk): `"Bu şifre sıfırlama bağlantısının süresi dolmuş."`

**Yetki:** Gerekmiyor

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `token` | string (UUID) | Evet | E-posta ile gönderilen sıfırlama token'ı |
| `newPassword` | string | Evet | Yeni şifre |

```http
POST /api/v1/auth/reset-password
Content-Type: application/json
```

```json
{
  "token": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "newPassword": "YeniGucluSifre456"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Şifreniz başarıyla güncellendi. Yeni şifrenizle giriş yapabilirsiniz.",
  "data": null,
  "timestamp": "2026-02-22T19:25:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `data` | null | Bu işlemde veri dönmez |

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

#### 10. `GET /api/v1/transactions/{id}` — Tekil İşlem Getir

Belirtilen ID'ye sahip işlemin detaylarını döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Getirilecek işlemin ID'si |

```http
GET /api/v1/transactions/eb85b6cc-7272-4c11-ba19-6f6829934c2c
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "İşlem detayları",
  "data": {
    "id": "eb85b6cc-7272-4c11-ba19-6f6829934c2c",
    "categoryName": "Market",
    "categoryIcon": "default-icon",
    "type": "EXPENSE",
    "amount": 2100.50,
    "description": "Haftalık market alışverişi",
    "transactionDate": "2026-02-09"
  },
  "timestamp": "2026-03-08T16:20:00.000000"
}
```

---

#### 11. `POST /api/v1/transactions/scan-receipt` — Fiş Tara (AI OCR)

Google Gemini 2.5 Flash kullanarak gönderilen fiş fotoğrafını analiz eder ve işlem bilgilerini döner. Bu endpoint sadece analiz yapar, veritabanına kayıt **eklemez**. Gelen veriler kullanıcı onayıyla `/api/v1/transactions` üzerinden kaydedilmelidir.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body (Multipart Form-Data):**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `file` | File (Image) | Evet | Fişin fotoğrafı (Max 20MB) |

```http
POST /api/v1/transactions/scan-receipt
Authorization: Bearer <Access_Token>
Content-Type: multipart/form-data
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Fiş başarıyla okundu",
  "data": {
    "amount": 450.75,
    "transactionDate": "2024-03-08",
    "description": "MİGROS TÜRK T.A.Ş.",
    "suggestedCategory": "MARKET"
  },
  "timestamp": "2026-03-08T15:30:00.000000"
}
```

**Response `data` Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `amount` | decimal | Okunan toplam tutar |
| `transactionDate` | string (YYYY-MM-DD) | Okunan fiş tarihi |
| `description` | string | Fişi kesen kurum/market adı |
| `suggestedCategory` | string | YZ tarafından önerilen kategori |

---

#### 12. `POST /api/v1/transactions/analyze-sms` — SMS Analiz Et (AI SMS)

Bankalardan veya diğer kurumlardan gelen SMS metinlerini analiz ederek finansal işlem detaylarını çıkarır.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `sender` | string | Evet | SMS'i gönderen başlık (Örn: ZIRAAT, ENPARA) |
| `smsText` | string | Evet | SMS'in tam metni |

```http
POST /api/v1/transactions/analyze-sms
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "sender": "BANKA",
  "smsText": "Sayin Musterimiz, 08/03/2026 tarihinde kartinizla 250.00 TL tutarinda MARKET harcamasi yapilmistir."
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "SMS başarıyla analiz edildi",
  "data": {
    "amount": 250.00,
    "transactionDate": "2026-03-08",
    "description": "MARKET",
    "suggestedCategory": "MARKET",
    "type": "EXPENSE"
  },
  "timestamp": "2026-03-08T16:10:00.000000"
}
```

**Response `data` Alanları:**

| `amount` | decimal | Analiz edilen tutar |
| `transactionDate` | string (YYYY-MM-DD) | Analiz edilen tarih |
| `description` | string | Analiz edilen kurum/yer adı |
| `suggestedCategory` | string | Önerilen kategori |
| `type` | string | İşlem tipi (`INCOME` veya `EXPENSE`) |

---

#### 12a. `POST /api/v1/transactions/analyze-gmail` — Gmail Analiz Et

Kullanıcının Google Access Token'ını kullanarak son X gündeki e-faturaları ve harcamaları tarar.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `accessToken` | string | Evet | Kullanıcının Google OAuth2 Access Token'ı |
| `daysToScan` | integer | Hayır | Geriye dönük kaç gün taranacak? (Varsayılan: 7) |

**Response (200 OK):**
Dönen veri `List<SmsAnalysisResponse>` tipindedir.

---

### 🎨 Layout (`/api/v1/user/layout`)

---

#### 26. `POST /api/v1/user/layout` — Arayüz Yerleşimini Kaydet

**Request Body:**
```json
{
  "widgets": ["BudgetSummary", "WealthOrbit", "RecentTransactions"]
}
```

---

#### 27. `GET /api/v1/user/layout` — Arayüz Yerleşimini Getir

---

### 🪐 Analytics (`/api/v1/analytics`)

---

#### 28. `GET /api/v1/analytics/orbit` — Wealth Orbit Verilerini Getir

Bütçelere göre "gezegen" hızlarını ve durumlarını döner.

**Response `data` Alanları:**
| Alan | Tip | Açıklama |
|------|-----|----------|
| `planetName` | string | Kategorinin adı |
| `currentAmount` | decimal | Mevcut harcama |
| `limit` | decimal | Bütçe limiti |
| `orbitSpeedMs` | long | Animasyon hızı (Harcama arttıkça azalır) |
| `isVibrating` | boolean | Limit aşıldı mı? |

---

### 🔒 Impulse Vault (`/api/v1/vault`)

---

#### 29. `POST /api/v1/vault` — Harcama Kilidi Oluştur

48 saatlik harcamama yemini kasası oluşturur.

**Request Body:**
```json
{
  "categoryId": 6,
  "amount": 500.0
}
```

---

### 🤖 AI Roast (`/api/v1/ai`)

---

#### 30. `GET /api/v1/ai/roast` — Finansal Roast Üret
Gemini tarafından üretilen eleştiriyi döner.

---

### 🎯 Goals & Habits (`/api/v1/goals` & `/api/v1/habits`)

---

#### 31. `GET /api/v1/habits/streak` — 30 Günlük Streak Map
Son 30 günün harcama profilini (harcamasız gün = true, harcamalı gün = false) `List<Boolean>` olarak döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "30 günlük alışkanlık serisi başarıyla getirildi.",
  "data": [
    true, true, false, true, false, true
  ],
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 32. `POST /api/v1/goals` — Yeni Hedef Oluştur

Kullanıcı için yeni bir finansal hedef (birikim vb.) oluşturur.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `title` | string | Evet | Hedef başlığı |
| `targetAmount` | decimal | Evet | Hedeflenen tutar |

```http
POST /api/v1/goals
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "title": "Araba Peşinatı",
  "targetAmount": 150000.00
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Finansal hedef başarıyla oluşturuldu.",
  "data": {
    "id": "e43b1a2d-4567-890a-bcde-123456789abc",
    "title": "Araba Peşinatı",
    "targetAmount": 150000.00,
    "savedAmount": 0.00,
    "completionPercentage": 0
  },
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 33. `GET /api/v1/goals` — Hedefleri Listele

Kullanıcının tüm hedeflerini listeler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Hedefler başarıyla getirildi.",
  "data": [
    {
      "id": "e43b1a2d-4567-890a-bcde-123456789abc",
      "title": "Araba Peşinatı",
      "targetAmount": 150000.00,
      "savedAmount": 15000.00,
      "completionPercentage": 10
    }
  ],
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 33a. `POST /api/v1/goals/{id}/add-savings` — Hedefe Birikim Ekle

Mevcut bir hedefe birikim miktarı ekler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**
| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Birikim eklenecek hedefin ID'si |

**Request Body:**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `amount` | decimal | Evet | Eklenecek birikim tutarı |

```http
POST /api/v1/goals/e43b1a2d-4567-890a-bcde-123456789abc/add-savings
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "amount": 5000.00
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Birikim hedefinize başarıyla eklendi.",
  "data": {
    "id": "e43b1a2d-4567-890a-bcde-123456789abc",
    "title": "Araba Peşinatı",
    "targetAmount": 150000.00,
    "savedAmount": 20000.00,
    "completionPercentage": 13
  },
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 33b. `PUT /api/v1/goals/{id}` — Hedef Güncelleme
Mevcut bir hedefi günceller.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**
| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Güncellenecek hedefin ID'si |

**Request Body:**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `title` | string | Evet | Hedef başlığı |
| `targetAmount` | decimal | Evet | Hedeflenen tutar |

```http
PUT /api/v1/goals/e43b1a2d-4567-890a-bcde-123456789abc
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "title": "Ev Peşinatı",
  "targetAmount": 300000.00
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Hedef başarıyla güncellendi.",
  "data": {
    "id": "e43b1a2d-4567-890a-bcde-123456789abc",
    "title": "Ev Peşinatı",
    "targetAmount": 300000.00,
    "savedAmount": 20000.00,
    "completionPercentage": 6
  },
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 33c. `DELETE /api/v1/goals/{id}` — Hedef Silme
Belirtilen ID'ye sahip hedefi siler.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**
| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Silinecek hedefin ID'si |

```http
DELETE /api/v1/goals/e43b1a2d-4567-890a-bcde-123456789abc
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Hedef başarıyla silindi.",
  "data": null,
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

### 📈 Budgets (`/api/v1/budgets`)

---

#### 13. `POST /api/v1/budgets` — Yeni Bütçe Oluştur

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

#### 14. `GET /api/v1/budgets` — Bütçeleri Listele

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

#### 15. `DELETE /api/v1/budgets/{id}` — Bütçeyi Sil

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

#### 16. `GET /api/v1/budgets/{id}` — Tekil Bütçe Getir

Belirtilen ID'ye sahip bütçenin detaylarını döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Getirilecek bütçenin ID'si |

```http
GET /api/v1/budgets/da022e27-6cf7-454c-a052-53964e1c7619
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bütçe detayları",
  "data": {
    "id": "da022e27-6cf7-454c-a052-53964e1c7619",
    "categoryName": "Market",
    "limitAmount": 2000,
    "spentAmount": 450.75,
    "percentage": 22.54,
    "period": "MONTHLY",
    "startDate": "2026-03-01",
    "endDate": "2026-03-31"
  },
  "timestamp": "2026-03-08T16:25:00.000000"
}
```

---

#### 17. `PUT /api/v1/budgets/{id}` — Bütçeyi Güncelle

Mevcut bir bütçeyi günceller.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Path Parameters:**

| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `id` | UUID | Güncellenecek bütçenin ID'si |

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `categoryId` | integer (Long) | Evet | Kategori ID'si |
| `amount` | decimal (BigDecimal) | Evet | Yeni bütçe limiti |
| `period` | string (enum) | Hayır | `WEEKLY`, `MONTHLY`, `YEARLY` |

```http
PUT /api/v1/budgets/da022e27-6cf7-454c-a052-53964e1c7619
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "categoryId": 6,
  "amount": 2500,
  "period": "MONTHLY"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Bütçe güncellendi",
  "data": {
    "id": "da022e27-6cf7-454c-a052-53964e1c7619",
    "categoryName": "Market",
    "limitAmount": 2500,
    "spentAmount": 450.75,
    "percentage": 18.03,
    "period": "MONTHLY",
    "startDate": "2026-03-01",
    "endDate": "2026-03-31"
  },
  "timestamp": "2026-03-08T16:26:00.000000"
}
```

---

### 📂 Categories (`/api/v1/categories`)

---

#### 23. `GET /api/v1/categories` — Tüm Kategorileri Listele

Sistemde tanımlı tüm kategorileri (varsayılan + eklenenler) id ve isim bilgisiyle birlikte döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

```http
GET /api/v1/categories
Authorization: Bearer <Access_Token>
```

**Response (200 OK):**

```json
{
  "success": true,
  "message": "Kategoriler listelendi",
  "data": [
    {
      "id": 1,
      "name": "Maaş",
      "type": "INCOME",
      "icon": "fa-wallet"
    },
    {
      "id": 6,
      "name": "Market",
      "type": "EXPENSE",
      "icon": "fa-cart-shopping"
    }
  ],
  "timestamp": "2026-03-05T17:31:00.000000"
}
```

**Response `data` Dizisi İçindeki Alanlar:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `id` | integer (Long) | Kategorinin benzersiz ID'si |
| `name` | string | Kategori adı (Maaş, Market vb.) |
| `type` | string (enum) | `INCOME` (Gelir) veya `EXPENSE` (Gider) |
| `icon` | string | Frontend ikon sınıfı |

---

### 🔔 Notifications (`/api/v1/notifications`)

---

#### 18. `GET /api/v1/notifications` — Bildirimleri Listele

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

#### 19. `GET /api/v1/notifications/unread-count` — Okunmamış Bildirim Sayısı

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

#### 20. `PUT /api/v1/notifications/{id}/read` — Bildirimi Okundu Olarak İşaretle

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

#### 21. `GET /api/v1/infra-test/ping` — Sistem Sağlık Kontrolü

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

#### 22. `GET /api/v1/infra-test/error-test` — Hata Testi

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
| 6 | `POST` | `/api/v1/auth/forgot-password` | Şifre sıfırlama bağlantısı gönder | ❌ |
| 7 | `POST` | `/api/v1/auth/reset-password` | Şifreyi sıfırla | ❌ |
| 8 | `POST` | `/api/v1/transactions` | Yeni işlem ekle | ✅ |
| 9 | `GET` | `/api/v1/transactions` | İşlemleri listele/filtrele | ✅ |
| 10 | `PUT` | `/api/v1/transactions/{id}` | İşlemi güncelle | ✅ |
| 11 | `DELETE` | `/api/v1/transactions/{id}` | İşlemi sil | ✅ |
| 12 | `GET` | `/api/v1/transactions/{id}` | Tekil işlem getir | ✅ |
| 13 | `POST` | `/api/v1/transactions/scan-receipt` | Fiş tara (OCR) | ✅ |
| 14 | `POST` | `/api/v1/transactions/analyze-sms` | SMS analiz et | ✅ |
| 15 | `POST` | `/api/v1/budgets` | Yeni bütçe oluştur | ✅ |
| 16 | `GET` | `/api/v1/budgets` | Bütçeleri listele | ✅ |
| 17 | `DELETE` | `/api/v1/budgets/{id}` | Bütçeyi sil | ✅ |
| 18 | `GET` | `/api/v1/budgets/{id}` | Tekil bütçe getir | ✅ |
| 19 | `PUT` | `/api/v1/budgets/{id}` | Bütçeyi güncelle | ✅ |
| 20 | `GET` | `/api/v1/notifications` | Bildirimleri listele | ✅ |
| 21 | `GET` | `/api/v1/notifications/unread-count` | Okunmamış bildirim sayısı | ✅ |
| 22 | `PUT` | `/api/v1/notifications/{id}/read` | Bildirimi okundu işaretle | ✅ |
| 23 | `GET` | `/api/v1/categories` | Tüm kategorileri listele | ✅ |
| 24 | `GET` | `/api/v1/infra-test/ping` | Sistem sağlık kontrolü | ❌ |
| 25 | `GET` | `/api/v1/infra-test/error-test` | Hata testi | ❌ |
| 26 | `POST` | `/api/v1/user/layout` | Layout kaydet | ✅ |
| 27 | `GET` | `/api/v1/user/layout` | Layout getir | ✅ |
| 28 | `GET` | `/api/v1/analytics/orbit` | Orbit verileri | ✅ |
| 29 | `POST` | `/api/v1/vault` | Harcama kilidi oluştur | ✅ |
| 30 | `GET` | `/api/v1/ai/roast` | AI Roast üret | ✅ |
| 31 | `GET` | `/api/v1/habits/streak` | 30 günlük heatmap | ✅ |
| 32 | `POST` | `/api/v1/goals` | Yeni hedef oluştur | ✅ |
| 33 | `GET` | `/api/v1/goals` | Hedefleri listele | ✅ |
| 33a | `POST` | `/api/v1/goals/{id}/add-savings` | Hedefe birikim ekle | ✅ |
| 33b | `PUT` | `/api/v1/goals/{id}` | Hedef güncelle | ✅ |
| 33c | `DELETE` | `/api/v1/goals/{id}` | Hedef sil | ✅ |
| 34 | `POST` | `/api/v1/transactions/analyze-gmail` | Gmail analiz et | ✅ |
| 34a | `GET` | `/api/v1/profile/me` | Profil bilgilerini getir | ✅ |
| 34b | `PUT` | `/api/v1/profile/me` | Profil bilgilerini güncelle | ✅ |
| 34c | `POST` | `/api/v1/profile/image` | Profil fotoğrafı yükle | ✅ |
| 34d | `GET` | `/api/v1/profile/image/{userId}` | Profil fotoğrafını getir | ❌ |
| 35 | `POST` | `/api/v1/users/fcm-token` | FCM Token kaydet/güncelle | ✅ |
| 36 | `POST` | `/api/v1/users/test-push` | Test push bildirimi gönder | ✅ |
| 37 | `DELETE` | `/api/v1/users/me` | Hesabı ve tüm verileri sil | ✅ |

---

### 👤 Profile API (`/api/v1/profile`)

---

#### 34a. `GET /api/v1/profile/me` — Profil Bilgilerini Getir
Giriş yapmış kullanıcının profil bilgilerini döner.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profil bilgileri başarıyla getirildi.",
  "data": {
    "id": "e43b1a2d-4567-890a-bcde-123456789abc",
    "fullName": "Alparslan Bozkurt",
    "email": "deneme1@arvenlabs.com",
    "age": 25,
    "income": 50000.00,
    "occupation": "Yazılım Mühendisi",
    "profileImageUrl": "/api/v1/profile/image/e43b1a2d-4567-890a-bcde-123456789abc",
    "createdAt": "2026-03-28T14:00:00"
  },
  "timestamp": "2026-03-28T14:00:00.000000"
}
```

---

#### 34b. `PUT /api/v1/profile/me` — Profil Bilgilerini Güncelle
Kullanıcının profil bilgilerini günceller. Yalnızca gönderilen alanlar güncellenir.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `fullName` | string | Hayır | Tam ad |
| `age` | integer | Hayır | Yaş |
| `income` | decimal | Hayır | Gelir |
| `occupation` | string | Hayır | Meslek |

```http
PUT /api/v1/profile/me
Authorization: Bearer <Access_Token>
Content-Type: application/json
```

```json
{
  "age": 26,
  "income": 60000.50
}
```

**Response (200 OK):**
*(Geriye güncellenmiş profil bilgilerini içeren bir response nesnesi döner)*

---

#### 34c. `POST /api/v1/profile/image` — Profil Fotoğrafı Yükle
Kullanıcının profil fotoğrafını yükler. Fotoğraf veritabanında (BYTEA) saklanır.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request (Multipart Form Data):**
| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `file` | file | Evet | Yüklenecek imaj dosyası |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profil fotoğrafı başarıyla yüklendi.",
  "data": null,
  "timestamp": "2026-03-31T15:00:00"
}
```

---

#### 34d. `GET /api/v1/profile/image/{userId}` — Profil Fotoğrafını Getir
Belirtilen kullanıcının profil fotoğrafını ham imaj verisi olarak döner.

**Yetki:** Gerekmiyor (Profil linki paylaşılabilir/görüntülenebilir)

**Path Parameters:**
| Parametre | Tip | Açıklama |
|-----------|-----|----------|
| `userId` | UUID | Fotoğrafı getirilecek kullanıcının ID'si |

**Response (200 OK):**
*   **Content-Type:** image/png, image/jpeg vb. (Orijinal dosya tipi)
*   **Body:** Ham imaj byte verisi.

---

### 📱 User & FCM Management (`/api/v1/users`)

---

#### 35. `POST /api/v1/users/fcm-token` — FCM Token Kaydet/Güncelle

Kullanıcının mobil cihazından aldığı Firebase Cloud Messaging (FCM) token'ını veritabanına kaydeder. Bildirim gönderimi için bu token kullanılır.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**
```json
{
  "token": "fcm_device_token_string_here"
}
```

---

---

#### 36. `POST /api/v1/users/test-push` — Test Push Bildirimi Gönder

Giriş yapmış olan kullanıcıya, kayıtlı token'ı üzerinden anlık test bildirimi gönderir.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Request Body:**
```json
{
  "title": "Test Başlığı",
  "message": "Bu bir test bildirim mesajıdır."
}
```

---

#### 37. `DELETE /api/v1/users/me` — Hesabı ve Tüm Verileri Sil

Kullanıcının hesabını ve bu hesaba bağlı tüm verileri (harcamalar, bütçeler, bildirimler, kilitler vb.) kalıcı olarak siler. Bu işlem geri alınamaz.

**Yetki:** Gerekli (`Authorization: Bearer <Access_Token>`)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Hesabınız ve tüm verileriniz kalıcı olarak silinmiştir.",
  "data": null,
  "timestamp": "2026-03-17T20:15:00.000000"
}
```

---

## ⚙️ Kurulum ve Çalıştırma

1. Projeyi bilgisayarınıza indirin.
2. PostgreSQL veritabanınızda `ceptefinans_db` adında boş bir veritabanı oluşturun.
3. `src/main/resources/application.properties` dosyasındaki veritabanı kullanıcı adı ve şifrenizi kendi yerel ortamınıza göre düzenleyin. (Bağlantı portu: 5433).
4. **Firebase:** `src/main/resources/firebase-service-account.json` dosyasının geçerli olduğundan emin olun.
5. Projeyi IDE'niz (IntelliJ IDEA vb.) üzerinden çalıştırın.
6. Veritabanı tabloları Hibernate (ddl-auto) tarafından otomatik oluşturulacak ve varsayılan kategoriler yüklenecektir.

---

*Gelecek Sürümlerde Planlananlar: Yapay Zeka (AI) destekli fiş okuma ve OCR entegrasyonu.*