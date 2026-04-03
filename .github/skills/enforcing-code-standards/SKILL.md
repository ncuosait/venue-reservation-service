---
name: enforcing-code-standards
description: 定義開發團隊的程式碼撰寫規範。每次與AI協作code撰寫或code review時，都應該要求AI讀取這份文件。主要包含繁體中文註解要求、命名慣例、語法風格等原則。
---

# 程式碼開發規範

本文件定義團隊開發的基準，旨在提升代碼的可讀性、可維護性

## 1. 命名慣例 (Naming Conventions)

- **變數與函式**：必須使用 `snake_case` (小寫單字並以底線分隔)。
    - **好的例子**：`user_profile`, `get_student_data`, `is_admin_authenticated`。
    - **壞的例子**：`userProfile`, `UserData`, `a1`。
- **具備意圖**：命名必須能反映變數的用途。避免使用模糊的縮寫。
- **常數**：全大寫並以底線分隔，例如 `MAX_RETRY_COUNT = 3`。

## 2. 語法與可讀性 (Syntax & Readability)

為了確保團隊成員都能輕鬆理解邏輯，我們優先選擇直觀的語法：

- **禁用進階匿名語法**：嚴格禁止使用 `lambda` 表達式。
- **使用傳統迴圈**：處理集合資料時，請使用傳統的 `for` 或 `while` 迴圈，而非列表推導式 (List Comprehensions) 或高階函式（如 map/filter），以提升邏輯追蹤的直覺性。
- **單一職責 (SRP)**：一個函式只做一件事。若函式超過 30 行，應考慮進行拆分。

## 3. 註解與文件 (Comments & Documentation)

- **語言要求**：所有的邏輯區塊、函式說明與變數註解必須使用 **繁體中文**。
- **註解格式**：
    - **函式註解**：每個函式必須有 docstring，說明其功能、參數與回傳值。
    - **行內註解**：對於複雜的邏輯或非直觀的程式碼行，必須加上行內註解說明。
    - **模組註解**：每個檔案開頭應有模組級別的註解，說明該檔案的主要功能與用途。
    - **段落註解**：在程式碼中適當位置加入段落註解，幫助區分不同邏輯區塊。使用 `# ===` 作為段落分隔符，並在上方加上簡短的標題說明該段落的功能。

  ```python
  # 範例
  # 函式註解
    def authenticate_user(self, data: UserLogin) -> Token:
        """
        使用者登入：驗證帳密、更新登入時間、簽發 Token
        參數 - data: 包含使用者輸入的帳號與密碼
        回傳 - Token: 成功登入後簽發的 JWT Token
        """
        ....
  # 段落註解
    class UserService:
    def __init__(self, repository: UserRepository, redis_client: Redis):
        self.repository = repository
        self.redis_client = redis_client

    # ==========================================
    # 1. 認證與登入 (Authentication)
    # ==========================================

    def authenticate_user(self, data: UserLogin) -> Token:
        """
        使用者登入：驗證帳密、更新登入時間、簽發 Token
        參數 - data: 包含使用者輸入的帳號與密碼
        回傳 - Token: 成功登入後簽發的 JWT Token
        """
    # ==========================================
    # 2. 使用者管理 (User Management)
    # ==========================================

    def get_users_list(self, current_user: User) -> List[UserRead]:
        """
        取得使用者列表：
        參數 - current_user: 當前登入的使用者物件
        回傳 - List[UserRead]: 根據使用者角色返回對應的使用
        """
  ```

- **第三方套件與 API**：凡是調用外部函式庫（如 `FastAPI`, `SQLAlchemy`）或外部 API 時，必須加上註解說明其用途與預期行為。
- **邏輯說明**：註解應解釋「為什麼」這樣寫，而不僅僅是「寫了什麼」。

## 4. 穩定性與架構原則 (Stability & Architecture)

參考 Clean Code 與 SOLID 原則，提升代碼的強健性與擴展能力：

- **錯誤處理 (Error Handling)**：
    - **嚴禁使用空的 `except: pass`**：必須捕獲特定的異常 (Exception)，並記錄具備追蹤價值的錯誤訊息。
    - **優先處理邊界情況**：在函式開頭就處理無效輸入（Guard Clauses），避免深層過度的 `if-else` 巢狀結構。
- **避免魔術數字 (No Magic Numbers)**：
    - 凡是具備特殊意義的數字或字串（如狀態碼、逾時時間），必須定義為具名常數或列舉 (Enum)。
- **依賴注入思維 (Dependency Inversion)**：
    - 函式需要的外部物件（如資料庫連線、API Client）應透過參數傳入，而非在函式內部直接實例化，以利於單元測試 (Unit Testing)。
- **保持擴展性 (Open/Closed Principle)**：
    - 系統設計應對「擴展」開放，對「修改」封閉。例如：處理不同單位的資料格式時，應透過介面或繼承邏輯，而非在主邏輯寫死大量的 `if-unit == 'A': ...`。
