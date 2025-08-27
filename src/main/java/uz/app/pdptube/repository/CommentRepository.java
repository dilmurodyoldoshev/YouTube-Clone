package uz.app.pdptube.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.app.pdptube.entity.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    Optional<List<Comment>> findAllByVideoId(Integer videoId);  // Video bo'yicha izohlarni olish
    void deleteAllByVideoId(Integer videoId);  // Video egasi uchun hamma izohlarni o'chirish
    void deleteByParentCommentId(Integer parentCommentId);
    boolean existsById(Integer id);
}
