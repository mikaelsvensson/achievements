import $ from "jquery";
import {get, post, put, remove, isLoggedIn} from "../util/api.jsx";
import {updateView, markdown2html} from "../util/view.jsx";

const templateImportScouternaSe = require("./import-scouterna-se.handlebars");
const templateForm = require("./_import-scouterna-se.form.handlebars");
const templateLoading = require("../loading.handlebars");

function achievementDtoToFormTemplateData (sourceData) {
    return {
        id: sourceData.id,
        inputs: {
            name: sourceData.name,
            image: sourceData.image,
            tags: (sourceData.tags || []).join(', '),
            description: (sourceData.description || "")
                .replace(/\n+/g, "\n\n")
                .trim(),
            steps: (sourceData.steps || [])
                .map(step => (step.id || 'ny') + ': ' + step.description)
                .join('\n')
        }
    }
}

function createSaveChangeResponseHandler($target) {
    return function (responseData, responseStatus, jqXHR) {
                           updateView(templateForm(Object.assign(achievementDtoToFormTemplateData(responseData),{
                               isNew: false,
                               isInBoth: true,
                               isOld: false
                           })), $target)
                       }
}

export function renderImportScouternaSe(appPathParams) {
    updateView(templateLoading());
    get('/api/achievements/scouterna-se-badges', function (responseData, responseStatus, jqXHR) {
        var allTags = {
            __isNew: '__ Nya',
            __isOld: '__ Gamla',
            __isInBoth: '__ Existerande',
            __diffs: '__ Existerande m. ändr.'
        }
        responseData.forEach(function (achievement) {
            var achievementTagKeys = []
            if (achievement.fromScouternaSe) {
                achievement.fromScouternaSe.tagsHtml = (achievement.fromScouternaSe.tags || []).join(', ')
                if (achievement.fromScouternaSe.description) {
                    achievement.fromScouternaSe.descriptionHtml = markdown2html(achievement.fromScouternaSe.description.replace(/\n/g, "\n\n").trim());
                }

                (achievement.fromScouternaSe.tags || []).forEach(tag => {
                    var key = 'achievement-tag-' + tag.replace(/[^a-z]/g, '')
                    achievementTagKeys.push(key)
                    allTags[key] = tag
                })
            }
            if (achievement.fromDatabase) {
                (achievement.fromDatabase.tags || []).forEach(tag => {
                    var key = 'achievement-tag-' + tag.replace(/[^a-z]/g, '')
                    achievementTagKeys.push(key)
                    allTags[key] = tag
                })
            }
            achievement.isNew = achievement.fromScouternaSe != null && achievement.fromDatabase == null
            achievement.isOld = achievement.fromScouternaSe == null && achievement.fromDatabase != null
            achievement.isInBoth = achievement.fromScouternaSe != null && achievement.fromDatabase != null
            achievementTagKeys.push(
                achievement.isNew
                    ? '__isNew'
                    : (achievement.isOld
                        ? '__isOld'
                        : (achievement.isInBoth
                            ? '__isInBoth'
                            : '')))
            var sourceData = achievement.isNew ? achievement.fromScouternaSe : achievement.fromDatabase
            Object.assign(achievement, achievementDtoToFormTemplateData(sourceData))

            if (achievement.diffs) {
                achievement.diffHtml = achievement.diffs
                    .map(diff => diff.change === 'ADDED'
                        ? `<ins>${diff.text}</ins>`
                        : (diff.change === 'REMOVED'
                            ? `<del>${diff.text}</del>`
                            : diff.text))
                    .join('')
                    .replace(/\n/g, '<br>')
                achievementTagKeys.push('__diffs')
            }
            achievement.tagKeysCssClasses = achievementTagKeys.join(' ')
        });

        updateView(templateImportScouternaSe({
            achievements: responseData,
            tags: Object
                .keys(allTags)
                .filter(key => key !== 'achievement-tag-')
                .map(key => ({
                    key: key,
                    label: allTags[key]
                }))
                .sort((a, b) => a.label.localeCompare(b.label))
        }));

        $('div.tags span.tag.achievement-tag').click(function () {
            var $this = $(this)
            var key = $this.data('key')
            if ($this.hasClass('is-dark')) {
                $('section.section-achievement').show()
                $this.removeClass('is-dark')
            } else {
                $('section.section-achievement').hide()
                $('section.section-achievement.' + key).show()
                $('span.tag.achievement-tag').removeClass('is-dark')
                $this.addClass('is-dark')
            }
        })

        $("button.import-upsert-achievement").click(function () {
            var $button = $(this)
            var $form = $button.closest("form")
            var id = $form.data('achievementId') || ''
            var name = $form.find("input.field-name").val()
            var tags = $form.find("input.field-tags").val().split(/\s*,\s*/)
            var image = $form.find("input.field-image").val()
            var description = $form.find("textarea.field-description").val()
            var steps = $form.find("textarea.field-steps").val()
               .split(/\n/)
               .filter(step => !!step)
               .map(step => {
                   var res = step.match(/^(ny|\d+): (.*)/)
                   return res[1] && res[2] ? [res[1], res[2]] : [] // Pass on only regex group 1 and 2 from result (if present)
               })
               .filter(pair => pair.length == 2)
               .map(pair => ({
                  id: pair[0] === 'ny' ? null : parseInt(pair[0]),
                  description: pair[1]
               }))

            var payload = {
                name,
                tags,
                image,
                description,
                steps
            }

            if (id) {
                put('/api/achievements/' + id,
                    payload,
                    createSaveChangeResponseHandler($form.parent()),
                    $button,
                    {
                        'Achievements-InProgressCheck': 'SKIP'
                    });
            } else {
                post('/api/achievements/' + id,
                    payload,
                    createSaveChangeResponseHandler($form.parent()),
                    $button);
                }
            })
        $("button.import-delete-achievement").click(function () {
            var $button = $(this)
            var $form = $button.closest("form")
            var id = $form.data('achievementId') || ''
            remove('/api/achievements/' + id, null, function (responseData, responseStatus, jqXHR) {
               $form.parent().remove()
            }, $button);
        })
    });
}