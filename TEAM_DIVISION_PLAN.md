# ClinicAI2: Exactly 25 Files Per Person (No Staff Admin Portal)

> [!NOTE]
> **No files have been deleted from your disk or project.** All files in `C:\Users\ASUS\AndroidStudioProjects\ClinicAI2\ClinicAI2` remain completely intact. 
> The **Staff Admin Portal has simply been excluded from the team division plan**, and Member 3 now owns the **Walk-in Queue & Check-In, Digital Receipts & PDF Generator, User Profile, and Clinic FAQs**.

---

## 1. Summary of 25-File Allocation (100% Non-Staff)

| Member | Focus Module | Breakdown | Total Files |
|---|---|---|:---:|
| **Member 1** | **Core Architecture, Auth & Doctor Search** | 3 Network/Config + 7 Auth Data/UI + 4 Doctor Data/Domain + 4 Doctor UI + 5 Security/Geo + 2 Layouts | **25** |
| **Member 2** | **Appointments, Booking Engine & AI Chatbot** | 4 Models + 3 Domain/Slots + 3 Repositories + 7 Booking/Appt UI + 3 AI Chat UI + 5 Layouts | **25** |
| **Member 3** | **Walk-in Queue, Receipts/PDF & User Profile** | 3 Models + 3 Repositories + 2 Local Prefs + 2 Domain + 2 PDF + 1 Token Parser + 8 UI Fragments/VMs + 4 Layouts | **25** |

---

## 2. Exact 25 Files Per Member (Zero Overlap & Zero Staff Files)

### Member 1: Core Architecture, Auth & Doctor Discovery (25 Files)

#### Config & Network
1. `app/src/main/java/com/example/clinicai/config/ApiConfig.kt`
2. `app/src/main/java/com/example/clinicai/config/AppConstants.kt`
3. `app/src/main/java/com/example/clinicai/data/remote/ApiClient.kt`
4. `app/src/main/java/com/example/clinicai/data/remote/ApiService.kt`
5. `app/src/main/java/com/example/clinicai/data/remote/SafeApiCall.kt`
6. `app/src/main/java/com/example/clinicai/data/remote/AuthInterceptor.kt`

#### Authentication & Security
7. `app/src/main/java/com/example/clinicai/data/auth/ClerkAuthManager.kt`
8. `app/src/main/java/com/example/clinicai/data/auth/AuthModels.kt`
9. `app/src/main/java/com/example/clinicai/data/auth/PasswordPolicy.kt`
10. `app/src/main/java/com/example/clinicai/ui/auth/AuthActivity.kt`
11. `app/src/main/java/com/example/clinicai/ui/auth/SignInFragment.kt`
12. `app/src/main/java/com/example/clinicai/ui/auth/SignUpFragment.kt`
13. `app/src/main/java/com/example/clinicai/ui/auth/AuthViewModel.kt`

#### Doctor & Clinic Discovery
14. `app/src/main/java/com/example/clinicai/data/model/Doctor.kt`
15. `app/src/main/java/com/example/clinicai/data/model/Clinic.kt`
16. `app/src/main/java/com/example/clinicai/data/model/Specialization.kt`
17. `app/src/main/java/com/example/clinicai/data/repository/DoctorRepository.kt`
18. `app/src/main/java/com/example/clinicai/data/repository/ClinicRepository.kt`
19. `app/src/main/java/com/example/clinicai/data/location/LocationRepository.kt`
20. `app/src/main/java/com/example/clinicai/domain/GeoDistance.kt`
21. `app/src/main/java/com/example/clinicai/domain/DoctorFilter.kt`
22. `app/src/main/java/com/example/clinicai/ui/doctors/FindDoctorFragment.kt`
23. `app/src/main/java/com/example/clinicai/ui/doctors/FindDoctorViewModel.kt`

#### Layouts
24. `app/src/main/res/layout/fragment_sign_in.xml`
25. `app/src/main/res/layout/fragment_find_doctor.xml`

---

### Member 2: Appointments, Booking Engine & AI Chat (25 Files)

#### Models & Domain
1. `app/src/main/java/com/example/clinicai/data/model/Appointment.kt`
2. `app/src/main/java/com/example/clinicai/data/model/Chat.kt`
3. `app/src/main/java/com/example/clinicai/data/model/Payment.kt`
4. `app/src/main/java/com/example/clinicai/data/model/Requests.kt`
5. `app/src/main/java/com/example/clinicai/domain/BookingDates.kt`
6. `app/src/main/java/com/example/clinicai/domain/SlotGrouper.kt`
7. `app/src/main/java/com/example/clinicai/domain/AppointmentItem.kt`

#### Repositories
8. `app/src/main/java/com/example/clinicai/data/repository/AppointmentRepository.kt`
9. `app/src/main/java/com/example/clinicai/data/repository/ChatRepository.kt`
10. `app/src/main/java/com/example/clinicai/data/repository/PaymentRepository.kt`

#### Booking & Appointment UI
11. `app/src/main/java/com/example/clinicai/ui/booking/BookingViewModel.kt`
12. `app/src/main/java/com/example/clinicai/ui/booking/BookingReviewFragment.kt`
13. `app/src/main/java/com/example/clinicai/ui/booking/BookingConfirmationFragment.kt`
14. `app/src/main/java/com/example/clinicai/ui/appointments/MyAppointmentsFragment.kt`
15. `app/src/main/java/com/example/clinicai/ui/appointments/AppointmentDetailFragment.kt`
16. `app/src/main/java/com/example/clinicai/ui/appointments/AppointmentsViewModel.kt`
17. `app/src/main/java/com/example/clinicai/ui/appointments/AppointmentAdapter.kt`

#### AI Chat Assistant UI
18. `app/src/main/java/com/example/clinicai/ui/chat/ChatFragment.kt`
19. `app/src/main/java/com/example/clinicai/ui/chat/ChatViewModel.kt`
20. `app/src/main/java/com/example/clinicai/ui/chat/ChatAdapter.kt`

#### Layouts
21. `app/src/main/res/layout/fragment_booking_review.xml`
22. `app/src/main/res/layout/fragment_booking_confirmation.xml`
23. `app/src/main/res/layout/fragment_my_appointments.xml`
24. `app/src/main/res/layout/fragment_chat.xml`
25. `app/src/main/res/layout/item_appointment.xml`

---

### Member 3: Walk-In Queue, Digital Receipts & User Profile (25 Files)

#### Models & Data Layer
1. `app/src/main/java/com/example/clinicai/data/model/Queue.kt`
2. `app/src/main/java/com/example/clinicai/data/model/UserProfile.kt`
3. `app/src/main/java/com/example/clinicai/data/model/Faq.kt`
4. `app/src/main/java/com/example/clinicai/data/repository/QueueRepository.kt`
5. `app/src/main/java/com/example/clinicai/data/repository/ReceiptRepository.kt`
6. `app/src/main/java/com/example/clinicai/data/repository/UserRepository.kt`
7. `app/src/main/java/com/example/clinicai/data/local/ReceiptStore.kt`
8. `app/src/main/java/com/example/clinicai/data/local/SessionPreferences.kt`

#### Domain & PDF Generation
9. `app/src/main/java/com/example/clinicai/domain/ReceiptBuilder.kt`
10. `app/src/main/java/com/example/clinicai/domain/ReceiptFormatter.kt`
11. `app/src/main/java/com/example/clinicai/pdf/ReceiptPdfGenerator.kt`
12. `app/src/main/java/com/example/clinicai/pdf/ReceiptFileManager.kt`
13. `app/src/main/java/com/example/clinicai/util/CheckinTokenParser.kt`

#### Walk-in Queue, Receipt & Profile UI
14. `app/src/main/java/com/example/clinicai/ui/checkin/CheckinFragment.kt`
15. `app/src/main/java/com/example/clinicai/ui/checkin/CheckinViewModel.kt`
16. `app/src/main/java/com/example/clinicai/ui/checkin/QueueTicketFragment.kt`
17. `app/src/main/java/com/example/clinicai/ui/booking/ReceiptFragment.kt`
18. `app/src/main/java/com/example/clinicai/ui/booking/ReceiptViewModel.kt`
19. `app/src/main/java/com/example/clinicai/ui/profile/ProfileFragment.kt`
20. `app/src/main/java/com/example/clinicai/ui/profile/ProfileViewModel.kt`
21. `app/src/main/java/com/example/clinicai/ui/faq/FaqFragment.kt`

#### Layouts
22. `app/src/main/res/layout/fragment_checkin.xml`
23. `app/src/main/res/layout/fragment_queue_ticket.xml`
24. `app/src/main/res/layout/fragment_receipt.xml`
25. `app/src/main/res/layout/fragment_profile.xml`

---

## 3. Git Push Commands

```bash
cd "C:\Users\ASUS\AndroidStudioProjects\ClinicAI2\ClinicAI2"
git init
```

### Initial Base Commit (Boilerplate)
```bash
git add .gitignore build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle/ app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/ app/src/main/res/navigation/ app/src/main/res/mipmap*
git commit -m "chore: initial project baseline and setup"
git remote add origin https://github.com/<your-username>/ClinicAI.git
git push -u origin main
```

### Member 1 (Exact 25 files)
```bash
git checkout -b feature/auth-and-doctor-search
git config user.name "Member 1 Name"
git config user.email "member1@example.com"

git add app/src/main/java/com/example/clinicai/config/ApiConfig.kt
git add app/src/main/java/com/example/clinicai/config/AppConstants.kt
git add app/src/main/java/com/example/clinicai/data/remote/ApiClient.kt
git add app/src/main/java/com/example/clinicai/data/remote/ApiService.kt
git add app/src/main/java/com/example/clinicai/data/remote/SafeApiCall.kt
git add app/src/main/java/com/example/clinicai/data/remote/AuthInterceptor.kt
git add app/src/main/java/com/example/clinicai/data/auth/ClerkAuthManager.kt
git add app/src/main/java/com/example/clinicai/data/auth/AuthModels.kt
git add app/src/main/java/com/example/clinicai/data/auth/PasswordPolicy.kt
git add app/src/main/java/com/example/clinicai/data/model/Doctor.kt
git add app/src/main/java/com/example/clinicai/data/model/Clinic.kt
git add app/src/main/java/com/example/clinicai/data/model/Specialization.kt
git add app/src/main/java/com/example/clinicai/data/repository/DoctorRepository.kt
git add app/src/main/java/com/example/clinicai/data/repository/ClinicRepository.kt
git add app/src/main/java/com/example/clinicai/data/location/LocationRepository.kt
git add app/src/main/java/com/example/clinicai/domain/GeoDistance.kt
git add app/src/main/java/com/example/clinicai/domain/DoctorFilter.kt
git add app/src/main/java/com/example/clinicai/ui/auth/AuthActivity.kt
git add app/src/main/java/com/example/clinicai/ui/auth/SignInFragment.kt
git add app/src/main/java/com/example/clinicai/ui/auth/SignUpFragment.kt
git add app/src/main/java/com/example/clinicai/ui/auth/AuthViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/doctors/FindDoctorFragment.kt
git add app/src/main/java/com/example/clinicai/ui/doctors/FindDoctorViewModel.kt
git add app/src/main/res/layout/fragment_sign_in.xml
git add app/src/main/res/layout/fragment_find_doctor.xml

git commit -m "feat(auth-doctors): add authentication and doctor search modules"
git push origin feature/auth-and-doctor-search
```

### Member 2 (Exact 25 files)
```bash
git checkout main
git pull origin main
git checkout -b feature/booking-and-chat
git config user.name "Member 2 Name"
git config user.email "member2@example.com"

git add app/src/main/java/com/example/clinicai/data/model/Appointment.kt
git add app/src/main/java/com/example/clinicai/data/model/Chat.kt
git add app/src/main/java/com/example/clinicai/data/model/Payment.kt
git add app/src/main/java/com/example/clinicai/data/model/Requests.kt
git add app/src/main/java/com/example/clinicai/domain/BookingDates.kt
git add app/src/main/java/com/example/clinicai/domain/SlotGrouper.kt
git add app/src/main/java/com/example/clinicai/domain/AppointmentItem.kt
git add app/src/main/java/com/example/clinicai/data/repository/AppointmentRepository.kt
git add app/src/main/java/com/example/clinicai/data/repository/ChatRepository.kt
git add app/src/main/java/com/example/clinicai/data/repository/PaymentRepository.kt
git add app/src/main/java/com/example/clinicai/ui/booking/BookingViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/booking/BookingReviewFragment.kt
git add app/src/main/java/com/example/clinicai/ui/booking/BookingConfirmationFragment.kt
git add app/src/main/java/com/example/clinicai/ui/appointments/MyAppointmentsFragment.kt
git add app/src/main/java/com/example/clinicai/ui/appointments/AppointmentDetailFragment.kt
git add app/src/main/java/com/example/clinicai/ui/appointments/AppointmentsViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/appointments/AppointmentAdapter.kt
git add app/src/main/java/com/example/clinicai/ui/chat/ChatFragment.kt
git add app/src/main/java/com/example/clinicai/ui/chat/ChatViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/chat/ChatAdapter.kt
git add app/src/main/res/layout/fragment_booking_review.xml
git add app/src/main/res/layout/fragment_booking_confirmation.xml
git add app/src/main/res/layout/fragment_my_appointments.xml
git add app/src/main/res/layout/fragment_chat.xml
git add app/src/main/res/layout/item_appointment.xml

git commit -m "feat(booking-chat): add appointment booking engine and AI chat assistant"
git push origin feature/booking-and-chat
```

### Member 3 (Exact 25 files - No Staff)
```bash
git checkout main
git pull origin main
git checkout -b feature/queue-receipts-profile
git config user.name "Member 3 Name"
git config user.email "member3@example.com"

git add app/src/main/java/com/example/clinicai/data/model/Queue.kt
git add app/src/main/java/com/example/clinicai/data/model/UserProfile.kt
git add app/src/main/java/com/example/clinicai/data/model/Faq.kt
git add app/src/main/java/com/example/clinicai/data/repository/QueueRepository.kt
git add app/src/main/java/com/example/clinicai/data/repository/ReceiptRepository.kt
git add app/src/main/java/com/example/clinicai/data/repository/UserRepository.kt
git add app/src/main/java/com/example/clinicai/data/local/ReceiptStore.kt
git add app/src/main/java/com/example/clinicai/data/local/SessionPreferences.kt
git add app/src/main/java/com/example/clinicai/domain/ReceiptBuilder.kt
git add app/src/main/java/com/example/clinicai/domain/ReceiptFormatter.kt
git add app/src/main/java/com/example/clinicai/pdf/ReceiptPdfGenerator.kt
git add app/src/main/java/com/example/clinicai/pdf/ReceiptFileManager.kt
git add app/src/main/java/com/example/clinicai/util/CheckinTokenParser.kt
git add app/src/main/java/com/example/clinicai/ui/checkin/CheckinFragment.kt
git add app/src/main/java/com/example/clinicai/ui/checkin/CheckinViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/checkin/QueueTicketFragment.kt
git add app/src/main/java/com/example/clinicai/ui/booking/ReceiptFragment.kt
git add app/src/main/java/com/example/clinicai/ui/booking/ReceiptViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/profile/ProfileFragment.kt
git add app/src/main/java/com/example/clinicai/ui/profile/ProfileViewModel.kt
git add app/src/main/java/com/example/clinicai/ui/faq/FaqFragment.kt
git add app/src/main/res/layout/fragment_checkin.xml
git add app/src/main/res/layout/fragment_queue_ticket.xml
git add app/src/main/res/layout/fragment_receipt.xml
git add app/src/main/res/layout/fragment_profile.xml

git commit -m "feat(queue-receipts-profile): add live queue ticket, PDF receipts and profile settings"
git push origin feature/queue-receipts-profile
```
