package uit.carbon_shop.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uit.carbon_shop.domain.Order;
import uit.carbon_shop.domain.Project;
import uit.carbon_shop.domain.ProjectReview;
import uit.carbon_shop.model.ProjectDTO;
import uit.carbon_shop.model.ProjectStatus;
import uit.carbon_shop.repos.AppUserRepository;
import uit.carbon_shop.repos.CompanyRepository;
import uit.carbon_shop.repos.OrderRepository;
import uit.carbon_shop.repos.ProjectRepository;
import uit.carbon_shop.repos.ProjectReviewRepository;
import uit.carbon_shop.util.NotFoundException;
import uit.carbon_shop.util.ReferencedWarning;


@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;
    private final ProjectMapper projectMapper;
    private final OrderRepository orderRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final RedissonClient redissonClient;

    public Page<ProjectDTO> findAll(final String filter, final Pageable pageable) {
        Page<Project> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = projectRepository.findAllById(longFilter, pageable);
        } else {
            page = projectRepository.findAll(pageable);
        }
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectDTO> findAllByOwner(final Long ownerCompany, String filter, final Pageable pageable) {
        final Page<Project> page =
                StringUtils.hasText(filter) ? projectRepository.findByOwnerCompany_IdAndNameContainsIgnoreCase(
                        ownerCompany, filter, pageable)
                        : projectRepository.findByOwnerCompany_Id(ownerCompany, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectDTO> findAllByOwnerAndStatus(final Long ownerCompany, ProjectStatus status, String filter,
            final Pageable pageable) {
        final Page<Project> page =
                StringUtils.hasText(filter) ? projectRepository.findByOwnerCompany_IdAndStatusAndNameContainsIgnoreCase(
                        ownerCompany, status, filter, pageable)
                        : projectRepository.findByOwnerCompany_IdAndStatus(ownerCompany, status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectDTO> findByStatus(ProjectStatus status, String filter, Pageable pageable) {
        final Page<Project> page =
                StringUtils.hasText(filter) ? projectRepository.findByStatusAndNameContainsIgnoreCase(
                        status, filter, pageable)
                        : projectRepository.findByStatus(status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectDTO> findByStatusButNotCompany(ProjectStatus status, Long companyId, String filter,
            Pageable pageable) {
        final Page<Project> page = StringUtils.hasText(filter)
                ? projectRepository.findByOwnerCompany_IdNotAndStatusAndNameContainsIgnoreCase(
                companyId, status, filter, pageable)
                : projectRepository.findByOwnerCompany_IdNotAndStatus(companyId, status, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public Page<ProjectDTO> findAllButNotCompany(Long companyId, String filter, Pageable pageable) {
        final Page<Project> page =
                StringUtils.hasText(filter) ? projectRepository.findByOwnerCompany_IdNotAndNameContainsIgnoreCase(
                        companyId, filter, pageable)
                        : projectRepository.findByOwnerCompany_IdNot(companyId, pageable);
        return new PageImpl<>(page.getContent()
                .stream()
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .toList(),
                pageable, page.getTotalElements());
    }

    public ProjectDTO get(final Long projectId) {
        return projectRepository.findById(projectId)
                .map(project -> projectMapper.updateProjectDTO(project, new ProjectDTO()))
                .orElseThrow(NotFoundException::new);
    }

    public Long create(final ProjectDTO projectDTO) {
        final Project project = new Project();
        project.setId(projectDTO.getProjectId());
        projectMapper.updateProject(projectDTO, project, companyRepository, appUserRepository);
        return projectRepository.save(project).getId();
    }

    public void update(final Long projectId, final ProjectDTO projectDTO) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        projectMapper.updateProject(projectDTO, project, companyRepository, appUserRepository);
        projectRepository.save(project);
    }

    public void updateStatus(final Long projectId, final ProjectStatus status) {
        RLock projectLock = redissonClient.getLock("PROJECT_LOCK:" + projectId);
        projectLock.lock();
        try {
            final Project project = projectRepository.findById(projectId)
                    .orElseThrow(NotFoundException::new);
            if (!project.getStatus().canUpdateTo(status)) {
                throw new IllegalArgumentException(
                        "Cannot update status from " + project.getStatus() + " to " + status);
            }
            project.setStatus(status);
            projectRepository.save(project);
        } finally {
            projectLock.unlock();
        }
    }

    public void approve(final Long projectId, final Long approveBy) {
        RLock projectLock = redissonClient.getLock("PROJECT_LOCK:" + projectId);
        projectLock.lock();
        try {
            final Project project = projectRepository.findById(projectId)
                    .orElseThrow(NotFoundException::new);
            if (!project.getStatus().canUpdateTo(ProjectStatus.APPROVED)) {
                throw new IllegalArgumentException(
                        "Cannot update status from " + project.getStatus() + " to " + ProjectStatus.APPROVED);
            }
            project.setStatus(ProjectStatus.APPROVED);
            project.setAuditBy(appUserRepository.findById(approveBy).orElseThrow(NotFoundException::new));
            projectRepository.save(project);
        } finally {
            projectLock.unlock();
        }
    }

    public void delete(final Long projectId) {
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        // remove many-to-many relations at owning side
        appUserRepository.findAllByFavoriteProjects(project)
                .forEach(appUser -> appUser.getFavoriteProjects().remove(project));
        projectRepository.delete(project);
    }

    public ReferencedWarning getReferencedWarning(final Long projectId) {
        final ReferencedWarning referencedWarning = new ReferencedWarning();
        final Project project = projectRepository.findById(projectId)
                .orElseThrow(NotFoundException::new);
        final Order projectOrder = orderRepository.findFirstByProject(project);
        if (projectOrder != null) {
            referencedWarning.setKey("project.order.project.referenced");
            referencedWarning.addParam(projectOrder.getId());
            return referencedWarning;
        }
        final ProjectReview projectProjectReview = projectReviewRepository.findFirstByProject(project);
        if (projectProjectReview != null) {
            referencedWarning.setKey("project.projectReview.project.referenced");
            referencedWarning.addParam(projectProjectReview.getId());
            return referencedWarning;
        }
        return null;
    }

}
