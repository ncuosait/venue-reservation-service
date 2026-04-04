---
name: api-doc-standard
description: 根據專案中當下的api設計與開發標準，撰寫符合規範的API文檔，確保前後端對接口的理解一致，並提供清晰的使用說明和示例。
---
# API 文檔撰寫標準
- 依據不同 module 進行分點 
- 每個分點必須包含以下內容
+ 該module的所有 api 端點(url)
+ 每個端點的功能說明
+ 每個端點的請求參數說明（包含參數名稱、類型、是否必填、說明）
+ 每個端點的成功回應格式說明（包含狀態碼、回應內容結構、字段說明）
+ 每個端點的失敗回應格式說明（包含狀態碼、回應內容結構、字段說明）
+ 每個端點的使用範例（包含請求示例和回應示例）
- 文檔內容必須清晰、簡潔，避免冗長的描述，確保前後端開發人員能夠快速理解和使用 API。
- 文檔格式應統一，使用 Markdown 或其他適合的格式進行撰寫，並且應該包含必要的標題、子標題和列表，以提高可讀性。
# 參考檔案
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/dto 定義了每個 api 的請求參數結構，必須撰寫該dto中所有字段的說明
- src/main/java/tw/edu/ncu/osa/venue_reservation_service/model/vo 定義了每個 api 的回應參數結構，必須撰寫該vo中所有字段的說明
- 任何以 Controller.java 結尾的檔案 : 每個 module 的 controller層都是對外提供api的地方，必須撰寫該controller中所有api的文檔
- 任何以 ServiceImpl.java 結尾的檔案 : 涉及具體的每個 api 業務實作細節
- 任何以 Mapper.xml 結尾的檔案 : 涉及具體的 SQL 邏輯，可以幫助理解每個 api 的數據處理邏輯，尤其是對於複雜的查詢和數據結構轉換。
- docs/Design/Base_Infrastructue : 定義了專案中統一的異常處理和響應格式，撰寫 API 文檔時必須參考該文檔中的規範，確保 API 的回應格式與專案標準一致。
