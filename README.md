## 清單
- compass: 傳統寫法, 純指北針
- android-gps-compass:
	- 大大的箭頭，應該是靠 GPS 指方向
	- 需要 GoogleLocationService
- Compass: 更簡單的傳統指南針，只有一個檔
- GPSLocation: 算是好的 GPS, 會跳出取得授權視窗，可以從網路撈位置
- Gps: 用 View-Model + Kotlin, 看不懂

## 想法
- GPS 採用 android-gps-compass/
- Compass 採用 compass
- 因為兩個都是用比較新的 androidx
