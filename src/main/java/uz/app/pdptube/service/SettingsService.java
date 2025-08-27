package uz.app.pdptube.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.app.pdptube.dto.SettingsDTO;
import uz.app.pdptube.entity.User;
import uz.app.pdptube.helper.Helper;
import uz.app.pdptube.payload.ResponseMessage;
import uz.app.pdptube.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;

    /**
     * 1) Foydalanuvchi joriy sozlamalarini olish
     */
    public ResponseMessage getSettings() {
        User currentUser = Helper.getCurrentPrincipal();

        // Sozlamalarni DTO ko‘rinishida qaytarish
        SettingsDTO settingsDTO = SettingsDTO.builder()
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .email(currentUser.getEmail())  // hozirgi email
                .password(currentUser.getPassword())
                .age(currentUser.getAge())
                .profilePicture(currentUser.getProfilePicture())
                .build();

        return new ResponseMessage(true, "Foydalanuvchi malumotlari muvaffaqiyatli olindi", settingsDTO);
    }


    /**
     * 2) Sozlamalarni yangilash (yangi email + yangi parol + boshqa ma’lumotlar)
     * Yangi emailga 4 xonali kod yuboramiz, to'g'ri kiritsa keyin confirm qilinadi.
     */
    public ResponseMessage updateSettings(SettingsDTO settingsDTO) {
        User currentUser = Helper.getCurrentPrincipal();


        // 2.1) Ism, familiya, age, profilePicture kabi oddiy maydonlarni darhol yangilaymiz
        if (settingsDTO.getFirstName() != null) {
            currentUser.setFirstName(settingsDTO.getFirstName());
        }
        if (settingsDTO.getLastName() != null) {
            currentUser.setLastName(settingsDTO.getLastName());
        }
        if (settingsDTO.getAge() != null) {
            currentUser.setAge(settingsDTO.getAge());
        }
        if (settingsDTO.getProfilePicture() != null) {
            currentUser.setProfilePicture(settingsDTO.getProfilePicture());
        }

        // 2.2) Agar foydalanuvchi "email" va "newPassword" ham jo‘natgan bo‘lsa:
        if (settingsDTO.getEmail() != null && settingsDTO.getPassword() != null) {
            /*
            // (a) Yangi parol murakkabligini tekshiramiz
            if (!isStrongPassword(settingsDTO.getPassword())) {
                return new ResponseMessage(false,
                        "Parol juda oddiy! Kamida 8 belgi, 1 ta katta harf, 1 ta kichik harf, " +
                                "1 ta raqam va 1 ta maxsus belgi bo‘lishi kerak.",
                        null
                );
            }*/

            // (b) Yangi email boshqa userga tegishli emasligini tekshirish
            Optional<User> byEmail = userRepository.findByEmail(settingsDTO.getEmail());
            if (byEmail.isPresent() && !byEmail.get().getId().equals(currentUser.getId())) {
                return new ResponseMessage(false, "Ushbu email allaqachon band", null);
            }
            currentUser.setPassword(settingsDTO.getPassword());
            currentUser.setEmail(settingsDTO.getEmail());
            userRepository.save(currentUser);
            return new ResponseMessage(true,"Sozlamalar yangilandi, endi qayta registratsiyadan o'tishni unitmang!", currentUser);
        } else {
            // Agar email yoki parol kiritilmagan bo‘lsa, demak faqat ism/familiya/age/rasm o‘zgardi
            userRepository.save(currentUser);
            return new ResponseMessage(true,
                    "Sozlamalar muvaffaqiyatli yangilandi (email/parol o‘zgarmadi).",
                    currentUser
            );
        }
    }
}
