package com.graduration.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.graduration.Constain.CommentTypeConstain;
import com.graduration.Repository.CommentRepository;
import com.graduration.Repository.LectureRepository;
import com.graduration.Repository.SubmissionRepository;
import com.graduration.Repository.UserRepository;
import com.graduration.Service.GradurationService.CommentService;
import com.graduration.entity.CommentEntity;
import com.graduration.entity.SubmistionEntity;
import com.graduration.entity.TeamEntity;
import com.graduration.entity.UserEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.SubmissionMapper;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock
    CommentRepository commentRepository;

    @Mock
    SubmissionRepository submissionRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    LectureRepository lectureRepository;

    @Mock
    SubmissionMapper submissionMapper;

    @InjectMocks
    CommentService commentService;

    @BeforeEach
    void authenticateAdmin() {
        authenticate("user-1", "ROLE_ADMIN");
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addComment_recordsAuthenticatedAuthorAndOrdinaryType() {
        SubmistionEntity submission = submission();
        UserEntity author =
                UserEntity.builder().userId("user-1").userName("admin").build();
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(author));
        when(commentRepository.save(any(CommentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.addComment(1L, " General feedback ");

        ArgumentCaptor<CommentEntity> captor = ArgumentCaptor.forClass(CommentEntity.class);
        verify(commentRepository).save(captor.capture());
        assertEquals("General feedback", captor.getValue().getContent());
        assertEquals(CommentTypeConstain.COMMENT, captor.getValue().getCommentType());
        assertEquals(author, captor.getValue().getCreatedBy());
    }

    @Test
    void updateComment_marksOwnedCommentAsEdited() {
        authenticate("lecturer-user", "ROLE_SUPERVISOR");
        CommentEntity comment = ordinaryComment("lecturer-user");
        when(commentRepository.findActiveById(2L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        commentService.updateComment(2L, "Updated feedback");

        assertEquals("Updated feedback", comment.getContent());
        assertEquals(true, comment.getEdited());
    }

    @Test
    void updateComment_rejectsDifferentAuthor() {
        authenticate("lecturer-user", "ROLE_SUPERVISOR");
        when(commentRepository.findActiveById(2L)).thenReturn(Optional.of(ordinaryComment("another-user")));

        AppException exception =
                assertThrows(AppException.class, () -> commentService.updateComment(2L, "Updated feedback"));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateComment_rejectsWorkflowComment() {
        CommentEntity comment = ordinaryComment("user-1");
        comment.setCommentType(CommentTypeConstain.APPROVAL);
        when(commentRepository.findActiveById(2L)).thenReturn(Optional.of(comment));

        AppException exception =
                assertThrows(AppException.class, () -> commentService.updateComment(2L, "Changed approval"));

        assertEquals(ErrorCode.COMMENT_OPERATION_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void deleteComment_softDeletesInsteadOfRemovingRow() {
        CommentEntity comment = ordinaryComment("user-1");
        when(commentRepository.findActiveById(2L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(2L);

        assertNotNull(comment.getDeletedAt());
        verify(commentRepository).save(comment);
    }

    private CommentEntity ordinaryComment(String authorId) {
        return CommentEntity.builder()
                .idComment(2L)
                .content("Feedback")
                .commentType(CommentTypeConstain.COMMENT)
                .createdBy(UserEntity.builder().userId(authorId).build())
                .edited(false)
                .build();
    }

    private SubmistionEntity submission() {
        return SubmistionEntity.builder()
                .IdSubmission(1L)
                .team(TeamEntity.builder().idTeam(10L).build())
                .build();
    }

    private void authenticate(String userId, String role) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId, null, java.util.List.of(new SimpleGrantedAuthority(role))));
    }
}
