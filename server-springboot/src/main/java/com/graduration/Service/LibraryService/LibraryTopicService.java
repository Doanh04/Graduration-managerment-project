package com.graduration.Service.LibraryService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.graduration.Configuration.PaginationSupport;
import com.graduration.DTO.Request.LibraryTopicRequest;
import com.graduration.DTO.Response.LibraryTopicResponse;
import com.graduration.Repository.LibraryTopicRepository;
import com.graduration.entity.LibraryTopicEntity;
import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;
import com.graduration.mapper.LibraryTopicMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LibraryTopicService {
    LibraryTopicRepository libraryTopicRepository;
    LibraryTopicMapper libraryTopicMapper;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public LibraryTopicResponse createLibraryTopic(LibraryTopicRequest request) {
        normalize(request);
        if (libraryTopicRepository.existsByTitleIgnoreCase(request.getTitle())) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_ALREADY_EXISTS);
        }

        LibraryTopicEntity libraryTopic = libraryTopicMapper.toLibraryTopicEntity(request);
        return libraryTopicMapper.toLibraryTopicResponse(libraryTopicRepository.save(libraryTopic));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public LibraryTopicResponse getLibraryTopic(Long idLibraryTopic) {
        return libraryTopicMapper.toLibraryTopicResponse(findLibraryTopic(idLibraryTopic));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<LibraryTopicResponse> getAllLibraryTopics() {
        return getAllLibraryTopics(0, PaginationSupport.DEFAULT_SIZE);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<LibraryTopicResponse> getAllLibraryTopics(Integer page, Integer size) {
        return libraryTopicRepository.findAll(PaginationSupport.pageRequest(page, size)).stream()
                .map(libraryTopicMapper::toLibraryTopicResponse)
                .toList();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public LibraryTopicResponse updateLibraryTopic(Long idLibraryTopic, LibraryTopicRequest request) {
        LibraryTopicEntity libraryTopic = findLibraryTopic(idLibraryTopic);
        normalize(request);
        if (libraryTopicRepository.existsByTitleIgnoreCaseAndIdLibraryTopicNot(request.getTitle(), idLibraryTopic)) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_ALREADY_EXISTS);
        }

        libraryTopicMapper.updateLibraryTopic(request, libraryTopic);
        return libraryTopicMapper.toLibraryTopicResponse(libraryTopicRepository.save(libraryTopic));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_FACULTY')")
    @Transactional
    public void deleteLibraryTopic(Long idLibraryTopic) {
        libraryTopicRepository.delete(findLibraryTopic(idLibraryTopic));
    }

    private LibraryTopicEntity findLibraryTopic(Long idLibraryTopic) {
        if (idLibraryTopic == null) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_NOT_FOUND);
        }
        return libraryTopicRepository
                .findById(idLibraryTopic)
                .orElseThrow(() -> new AppException(ErrorCode.LIBRARY_TOPIC_NOT_FOUND));
    }

    private void normalize(LibraryTopicRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new AppException(ErrorCode.LIBRARY_TOPIC_TITLE_NOT_BLANK);
        }
        request.setTitle(request.getTitle().trim());
        request.setDescription(normalizeNullable(request.getDescription()));
        request.setObjective(normalizeNullable(request.getObjective()));
        request.setTechnology(normalizeNullable(request.getTechnology()));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
