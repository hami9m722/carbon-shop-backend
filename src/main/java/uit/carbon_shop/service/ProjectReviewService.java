package uit.carbon_shop.service;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uit.carbon_shop.domain.ProjectReview;
import uit.carbon_shop.model.ProjectReviewDTO;
import uit.carbon_shop.repos.AppUserRepository;
import uit.carbon_shop.repos.ProjectRepository;
import uit.carbon_shop.repos.ProjectReviewRepository;
import uit.carbon_shop.util.NotFoundException;


@Service
@Transactional
public class ProjectReviewService {

    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectReviewMapper projectReviewMapper;

    public ProjectReviewService(final ProjectReviewRepository projectReviewRepository,
            final ProjectRepository projectRepository, final AppUserRepository appUserRepository,
            final ProjectReviewMapper projectReviewMapper) {
        this.projectReviewRepository = projectReviewRepository;
        this.projectRepository = projectRepository;
        this.appUserRepository = appUserRepository;
        this.projectReviewMapper = projectReviewMapper;
    }

    public Page<ProjectReviewDTO> findAll(final String filter, final Pageable pageable) {
        Page<ProjectReview> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = projectReviewRepository.findAllById(longFilter, pageable);
        } else {
            page = projectReviewRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(projectReview -> projectReviewMapper.updateProjectReviewDTO(projectReview, new ProjectReviewDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectReviewDTO> findAllByProject(final Long projectId, final Pageable pageable) {
        final Page<ProjectReview> page = projectReviewRepository.findByProject_Id(projectId, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(projectReview -> projectReviewMapper.updateProjectReviewDTO(projectReview, new ProjectReviewDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public int getLikeCount(final Long id) {
        return projectReviewRepository.findById(id).map(ProjectReview::getLikeBy).orElseThrow(NotFoundException::new).size();
    }

    public ProjectReviewDTO get(final Long id) {
        return projectReviewRepository.findById(id)
                .map(projectReview -> projectReviewMapper.updateProjectReviewDTO(projectReview, new ProjectReviewDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProjectReviewDTO projectReviewDTO) {
        final ProjectReview projectReview = new ProjectReview();
        projectReview.setId(projectReviewDTO.getId());
        projectReviewMapper.updateProjectReview(projectReviewDTO, projectReview, projectRepository, appUserRepository);
        return projectReviewRepository.save(projectReview).getId();
    }

    public void update(final Long id, final ProjectReviewDTO projectReviewDTO) {
        final ProjectReview projectReview = projectReviewRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        projectReviewMapper.updateProjectReview(projectReviewDTO, projectReview, projectRepository, appUserRepository);
        projectReviewRepository.save(projectReview);
    }

    public void delete(final Long id) {
        final ProjectReview projectReview = projectReviewRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        // remove many-to-many relations at owning side
        appUserRepository.findAllByLikeProjectReviews(projectReview)
                .forEach(appUser -> appUser.getLikeProjectReviews().remove(projectReview));
        projectReviewRepository.delete(projectReview);
    }

}
