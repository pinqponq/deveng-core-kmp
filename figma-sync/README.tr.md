# Figma ↔ Compose Sync Sistemi

## Dökümanın Amacı

Bu döküman, Jetpack Compose bileşenlerini Code Connect kullanarak Figma ile senkronize etmek için kapsamlı bir rehber sunar. Bu dökümanı okuduktan sonra şunları yapabileceksiniz:

- Kotlin composable fonksiyonlarından schema çıkarmak
- Component manifest'lerini Figma URL'leri ile yapılandırmak
- Code Connect template'leri oluşturmak
- Uyumsuzlukları tespit etmek için drift audit çalıştırmak
- Bileşenleri Figma'ya yayınlamak

> 📌 Bu dökümanın İngilizce versiyonu için [README.md](./README.md) dosyasına bakınız.

---

## İçindekiler

1. [Ön Gereksinimler](#1-ön-gereksinimler)
2. [Temel Kavramlar](#2-temel-kavramlar)
3. [Hızlı Başlangıç](#3-hızlı-başlangıç)
4. [Adım Adım İş Akışı](#4-adım-adım-iş-akışı)
5. [CLI Referansı](#5-cli-referansı)
6. [Type Mapping Referansı](#6-type-mapping-referansı)
7. [Sorun Giderme](#7-sorun-giderme)
8. [Dizin Yapısı](#8-dizin-yapısı)

---

## 1. Ön Gereksinimler

Bu sistemi kullanmadan önce aşağıdakilerin kurulu olduğundan emin olun:

| Gereksinim | Açıklama |
|------------|----------|
| Node.js & npm | Code Connect CLI için gerekli |
| Figma Access Token | `FIGMA_ACCESS_TOKEN` ortam değişkeni olarak ayarlanmalı |
| Gradle | Schema extraction ve template generation için gerekli |

**Kurulum komutları:**

```bash
# npm bağımlılıklarını yükle
npm install

# Figma token'ı ayarla (drift audit ve publish için gerekli)
export FIGMA_ACCESS_TOKEN="your-token-here"
```

---

## 2. Temel Kavramlar

### 2.1 Schema

Bileşen parametrelerini ve Figma binding'lerini tanımlayan JSON dosyasıdır. Schema, Kotlin fonksiyon parametrelerini Figma property'lerine eşler.

### 2.2 Manifest

`components.manifest.json` dosyası, bileşenleri Figma URL'leri ve template dosya yolları ile kaydeder.

### 2.3 Template

Figma Dev Mode'da görüntülenen Compose kod parçacıklarını üreten JavaScript dosyalarıdır (`.figma.template.js`).

### 2.4 Binding Türleri

| Binding | Amacı |
|---------|-------|
| `TEXT_CHARACTERS` | `#bind:paramName` marker'ı ile text layer içeriğine bağlanır |
| `VARIANT_AXIS` | Figma variant property'lerine eşlenir |
| `INSTANCE_SWAP` | `#swap:paramName` marker'ı ile icon instance'larını değiştirir |
| `PROP_ONLY` | Yalnızca kod parametresi, Figma layer binding'i yok |
| `NONE` | Binding'lerden hariç tutulur (callback'ler) |

### 2.5 Layer Marker'ları

| Marker | Amacı | Örnek |
|--------|-------|-------|
| `#bind:paramName` | Text içerik binding'i | Layer: `Label #bind:label` |
| `#swap:paramName` | Instance swap slot'u | Layer: `Icon #swap:icon` |

### 2.6 İç İçe Geçebilen Bileşenler (Nestable)

Composable içerik (slot) kabul eden bileşenler schema'da `nestable: true` olarak işaretlenir. Bu, Figma Code Connect'te alt bileşenler içermelerine olanak tanır.

Schema CLI slot parametrelerini otomatik olarak algılar:
- `Slot` tipi (`@Composable () -> Unit` için typealias)
- `@Composable () -> Unit` lambda parametreleri
- `@Composable SomeScope.() -> Unit` kapsamlı lambda parametreleri

**Örnek:**

```kotlin
@Composable
fun Container(
    label: String,
    content: Slot  // Bu bileşeni nestable yapar
) { ... }
```

Şu schema'yı üretir:

```json
{
  "componentName": "Container",
  "codeConnect": {
    "nestable": true
  }
}
```

---

## 3. Hızlı Başlangıç

### 3.1 Tam Pipeline (Tek Komut)

```bash
./gradlew figmaSync
```

Bu komut:
1. Tüm `@Composable` bileşenlerini ve `DrawableResource` dosyalarını keşfeder
2. Birleştirilmiş bir `component-schema.json` oluşturur
3. `components.manifest.json` dosyasını günceller (yer tutucu oluşturur veya interaktif giriş alır)
4. `figma-sync/templates/icons/` dizininde drawable template'leri oluşturur
5. Tüm bileşenler için Code Connect template'lerini oluşturur
6. Figma'ya karşı drift audit çalıştırır

### 3.2 Tüm Bileşenleri Keşfet (Varsayılan)

Argüman verilmediğinde, CLI projedeki tüm `@Composable` fonksiyonlarını ve `DrawableResource` dosyalarını otomatik olarak keşfeder:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema
```

Bu komut:
1. `deveng-core/src/commonMain/kotlin/core/presentation/component/` dizinini `@Composable` fonksiyonları içeren Kotlin dosyaları için tarar
2. Drawable dizinlerini icon kaynakları için tarar (`ic_*.xml`, `shared_ic_*.xml`, vb.)
3. Keşfedilen tüm bileşenlerle birleştirilmiş bir `component-schema.json` oluşturur
4. Her keşfedilen drawable için `figma-sync/templates/icons/` dizininde template dosyaları oluşturur

### 3.3 Tek Kotlin Dosyasından Schema Çıkarma

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--input deveng-core/src/commonMain/kotlin/core/presentation/component/YourComponent.kt"
```

### 3.4 Manifest Oluşturma/Güncelleme

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
# Figma URL'lerini sorması için -Pinteractive=true ekleyin
```

Çıktı: `figma-sync/schema/components.manifest.json` dosyasını günceller; schemada olup manifestte olmayan bileşenler için giriş ekler ve URL verilmezse yer tutucular (`<PASTE_FIGMA_URL_HERE>`, `<FILE_KEY>`, `<NODE_ID>`) yazar.

### 3.5 Template Oluşturma

```bash
./gradlew :figma-sync:tools:template-generator:generateTemplates
```

> Not: Bu görev yalnızca template üretir. `figma-sync/schema/component-schema.json` dosyasının güncel olduğundan emin olun (önce `generateSchema` veya `generateSchemaManuel` çalıştırın).

### 3.6 Figma'ya Yayınlama

```bash
npm run codeconnect:publish
```

---

## 4. Adım Adım İş Akışı

### Adım 1: Schema Oluşturma

**Seçenek A: Tüm Bileşenleri Keşfet (Tam Senkronizasyon İçin Önerilen)**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema
```

Argüman verilmediğinde, CLI:
- Bileşen dizinini `@Composable` fonksiyonları içeren `.kt` dosyaları için özyinelemeli olarak tarar
- Icon desenlerine uyan drawable kaynaklarını keşfeder (`ic_*.xml`, `shared_ic_*.xml`, vb.)
- Tüm bileşenlerle tek bir birleştirilmiş schema oluşturur
- Her drawable için `figma-sync/templates/icons/` dizininde template dosyaları oluşturur

Arama dizinlerini özelleştirebilirsiniz:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--component-dir ozel/yol/bilesenler --drawable-dir yol/drawables"
```

**Seçenek B: Tek Kotlin Dosyasından Çıkarma**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchema \
  -Pargs="--input path/to/YourComponent.kt"
```

Extractor şunları yapar:
- Fonksiyon signature ve parametrelerini parse eder
- `type-mapping.json` kullanarak Kotlin type'larını eşler
- Hariç tutulan type'ları filtreler (Modifier, Color, TextStyle, Shape, Dp)
- Uygun binding'leri oluşturur

**Örnek girdi:**

```kotlin
@Composable
fun CustomIconButton(
    modifier: Modifier = Modifier,      // Hariç tutulur
    isEnabled: Boolean = true,          // VARIANT_AXIS
    icon: DrawableResource,             // INSTANCE_SWAP
    iconDescription: String,            // PROP_ONLY
    onClick: () -> Unit                 // EXCLUDED
)
```

**Seçenek C: Manuel Yazım**

`schema/component-schema.raw.json` dosyasını doğrudan düzenleyin, sonra canonicalize edin:

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel \
  --args="--raw figma-sync/schema/component-schema.raw.json"
```

### Adım 2: Manifest Yapılandırma

Tüm keşfedilen bileşenler için manifest girişlerini üretin veya güncelleyin (eksik olanlara stub yazar):

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
# Figma URL'lerini adım adım girmek için --interactive ekleyin
```

Bu komut:
- `figma-sync/schema/component-schema.json` dosyasını okur ve her bileşen için manifest girişi olduğundan emin olur.
- Var olan manifest girişlerini korur (güncel schemada olmayan bileşenler dahil).
- Yeni bileşenler için yer tutucular yazar: `componentUrl: "<PASTE_FIGMA_URL_HERE>"`, `fileKey: "<FILE_KEY>"`, `nodeId: "<NODE_ID>"`.
- `--interactive` verilirse Figma URL'ini sorar ve `fileKey`/`nodeId` değerlerini otomatik doldurur.

Komut sonrası `schema/components.manifest.json` dosyasını açıp gerçek Figma URL'leri ile yer tutucuları doldurun ve sonra template üretin.

Bileşeninizi `schema/components.manifest.json` dosyasına ekleyin veya düzenleyin:

```json
{
  "componentName": "YourComponent",
  "kotlinFqName": "core.presentation.component.YourComponent",
  "codeConnect": {
    "templateFile": "figma-sync/templates/YourComponent.figma.template.js",
    "publish": { "source": "template-v2" }
  },
  "figma": {
    "fileKey": "YOUR_FILE_KEY",
    "nodeId": "NODE-ID",
    "componentUrl": "https://www.figma.com/design/YOUR_FILE_KEY/Design-System?node-id=NODE-ID"
  }
}
```

**Figma URL'i Alma:**
1. Figma dosyasını açın
2. Bileşeni seçin
3. Sağ tık → "Copy link to selection"
4. URL'den `fileKey` ve `nodeId` değerlerini çıkarın

### Adım 3: Template Oluşturma

```bash
./gradlew :figma-sync:tools:template-generator:generateTemplates
```

Çıktı: `templates/YourComponent.figma.template.js`

> Not: Bu görev yalnızca template üretir. `figma-sync/schema/component-schema.json` dosyasının güncel olduğundan emin olun (önce `generateSchema` veya `generateSchemaManuel` çalıştırın).

### Adım 4: Drift Audit Çalıştırma

```bash
export FIGMA_ACCESS_TOKEN="your-token"
./gradlew :figma-sync:tools:drift-auditor:auditDrift
```

Sonuçları `schema/drift-report.md` dosyasında inceleyin.

### Adım 5: Doğrulama ve Yayınlama

```bash
# Template'leri doğrula
npm run codeconnect:parse

# Figma'ya yayınla
npm run codeconnect:publish
```

---

## 5. CLI Referansı

### 5.1 Gradle Task'ları

| Task | Açıklama |
|------|----------|
| `figmaSync` | Tam pipeline: schema → templates → audit |
| `generateSchema` | Kotlin veya drawable'dan schema çıkar |
| `generateManifest` | Schema'dan `components.manifest.json` üretir/günceller (eksik URL'ler için stub) |
| `generateSchemaManuel` | Raw JSON'u canonicalize et |
| `generateTemplates` | Mevcut schema'dan Code Connect template'lerini oluşturur (schema görevi çalıştırmaz) |
| `auditDrift` | Schema'yı Figma bileşenleri ile karşılaştır |

**Task seçenekleri (Gradle CLI):**
- Manifest: `./gradlew :figma-sync:tools:manifest-cli:generateManifest --interactive --schema <path> --manifest <path>`
- Template: `./gradlew :figma-sync:tools:template-generator:generateTemplates --schema <path> --manifest <path> --templates <outDir>`
- Schema (tümünü keşfet): `./gradlew :figma-sync:tools:schema-cli:generateSchema --discover-all --component-dir <dir> --drawable-dir <dir> --schema-out <path> --mapping <path> --overrides <path>`
- Schema (tek dosya): `./gradlew :figma-sync:tools:schema-cli:generateSchema --input <file> --component <name> --figma-url <url> --schema-out <path> --template-out <path>`
- Schema canonicalize: `./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel --mode=canonicalize --raw <path> --out <path> --mapping <path> --overrides <path>`
- Drift: `./gradlew :figma-sync:tools:drift-auditor:auditDrift --schema <path> --manifest <path> --report-json <path> --report-md <path>`

`generateManifest` görevi `figma-sync:tools:manifest-cli` modülündedir. Çalıştırmak için:

```bash
./gradlew :figma-sync:tools:manifest-cli:generateManifest
```

### 5.2 generateSchema Argümanları

**Genel Seçenekler:**

| Argüman | Açıklama |
|---------|----------|
| `--help`, `-h` | Tüm mevcut seçenekleri içeren yardım mesajını gösterir |

**Tümünü Keşfet Modu (`--input` verilmediğinde varsayılan):**

| Argüman | Zorunlu | Varsayılan | Açıklama |
|---------|---------|------------|----------|
| `--discover-all` | | | Tümünü keşfet modunu açıkça tetikler |
| `--component-dir <path>` | | `deveng-core/src/commonMain/kotlin/core/presentation/component` | `@Composable` keşfi için kök dizin |
| `--drawable-dir <path>` | | `deveng-core/src/commonMain/composeResources/drawable`, `sample/composeApp/src/commonMain/composeResources/drawable` | Taranacak drawable dizini (birden fazla dizin için tekrarlanabilir) |
| `--schema-out <path>` | | `figma-sync/schema/component-schema.json` | Final schema JSON çıktı yolu |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping yapılandırma dosyası |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Manuel düzeltmeler için schema overrides dosyası |

**Tek Dosya Çıkarma Modu:**

| Argüman | Zorunlu | Varsayılan | Açıklama |
|---------|---------|------------|----------|
| `--input <path>` | ✅ | | Kotlin dosyası (`.kt`) veya drawable kaynak yolu |
| `--component <name>` | | | Çıkarılacak bileşen adı (dosyada birden fazla `@Composable` fonksiyon varsa) |
| `--figma-url <url>` | | | Figma bileşen URL'i (drawable modu için, verilmezse interaktif olarak sorar) |
| `--schema-out <path>` | | `figma-sync/schema/component-schema.json` | Schema JSON çıktı yolu (mevcut dosyayı değiştirir) |
| `--template-out <path>` | | `figma-sync/templates/icons/<IconName>.figma.template.js` | Drawable template çıktı yolu |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping yapılandırma dosyası |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Schema overrides dosyası |

### 5.3 generateSchemaManuel Argümanları

Raw JSON'dan canonicalize edilmiş schema iş akışı için kullanılır (manuel/AI destekli yazım):

| Argüman | Zorunlu | Varsayılan | Açıklama |
|---------|---------|------------|----------|
| `--mode=canonicalize` | | | Yalnızca canonicalize modunu zorla (`--raw` alternatifi) |
| `--raw <path>` | ✅ | | Raw schema JSON girdi dosyası |
| `--out <path>` | | `figma-sync/schema/component-schema.json` | Final schema JSON çıktı yolu |
| `--overrides <path>` | | `figma-sync/schema/schema.overrides.json` | Schema overrides dosyası |
| `--mapping <path>` | | `figma-sync/schema/type-mapping.json` | Type mapping dosyası (doğrulama için) |

**Örnek:**

```bash
./gradlew :figma-sync:tools:schema-cli:generateSchemaManuel \
  -Pargs="--raw figma-sync/schema/component-schema.raw.json --out figma-sync/schema/component-schema.json"
```

### 5.4 npm Script'leri

| Script | Açıklama |
|--------|----------|
| `npm run codeconnect:parse` | Template'leri doğrula |
| `npm run codeconnect:publish` | Figma'ya yayınla |

---

## 6. Type Mapping Referansı

Dosya: `schema/type-mapping.json`

| Kotlin Type | Schema Kind | Binding |
|-------------|-------------|---------|
| `String` | TEXT | TEXT_CHARACTERS |
| `Boolean` | BOOLEAN | VARIANT_AXIS |
| `DrawableResource` | INSTANCE_SWAP | INSTANCE_SWAP |
| `() -> Unit` | EXCLUDED | NONE |
| `(Boolean) -> Unit` | EXCLUDED | NONE |
| `Int`, `Float` | TEXT | PROP_ONLY |

**Hariç tutulan type'lar:** `Modifier`, `Color`, `TextStyle`, `Shape`, `Dp`

**Yeni type ekleme:**

```json
{
  "MyCustomType": {
    "kind": "TEXT",
    "binding": "PROP_ONLY",
    "supportsLiteralDefault": true,
    "literalDefaultType": "string"
  }
}
```

---

## 7. Sorun Giderme

| Sorun | Çözüm |
|-------|-------|
| `FIGMA_ACCESS_TOKEN not set` | Çalıştırın: `export FIGMA_ACCESS_TOKEN="..."` |
| Schema'da parametre eksik | Kotlin type için `type-mapping.json` dosyasını kontrol edin |
| Drift: property not found | Figma bileşeninde eşleşen variant adlarını doğrulayın |
| Template parse error | JS syntax'ını kontrol edin; `url=` comment'inin ilk satırda olduğundan emin olun |
| Publish başarısız | Bağımlılıkları doğrulamak için `npm install` çalıştırın |

---

## 8. Dizin Yapısı

```
figma-sync/
├── schema/
│   ├── component-schema.json       # Canonicalize edilmiş schema
│   ├── component-schema.raw.json   # Raw schema (manuel iş akışı)
│   ├── components.manifest.json    # Bileşen kaydı
│   ├── schema.overrides.json       # Override kuralları
│   ├── type-mapping.json           # Kotlin type eşlemeleri
│   ├── drift-report.json           # Audit sonuçları (JSON)
│   └── drift-report.md             # Audit sonuçları (Markdown)
├── templates/
│   ├── *.figma.template.js         # Bileşen template'leri
│   └── icons/                      # Icon template'leri
└── tools/
    ├── schema-cli/                 # Schema extraction
    ├── template-generator/         # Template generation
    └── drift-auditor/              # Drift detection
```

---

## İş Akışı Özeti

```
┌────────────────────────────────────────────────────────────┐
│  1. SCHEMA OLUŞTUR                                         │
│     generateSchema (tümünü keşfet) veya --input Component.kt │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  2. MANIFEST YAPILANDIR                                    │
│     components.manifest.json'a kayıt ekle                  │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  3. TEMPLATE OLUŞTUR                                       │
│     generateTemplates                                      │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  4. DRIFT AUDIT                                            │
│     auditDrift → drift-report.md'yi incele                 │
└─────────────────────────┬──────────────────────────────────┘
                          ▼
┌────────────────────────────────────────────────────────────┐
│  5. YAYINLA                                                │
│     npm run codeconnect:publish                            │
└────────────────────────────────────────────────────────────┘
```

---

**Deveng Group - Figma Sync Ekibi**

